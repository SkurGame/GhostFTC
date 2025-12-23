package org.firstinspires.ftc.teamcode.lib;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GhostTrajectoryRecorder {

    public static class MotorConfig {
        public String name;
        public double speed;
        public boolean reversed;
        public DcMotorEx motor;
        public MotorConfig(String name, double speed, boolean reversed) {
            this.name = name; this.speed = speed; this.reversed = reversed;
        }
    }

    public static class TrajectoryPoint {
        public int stepNumber;
        public long timestamp;
        public Map<String, Integer> motorPositions = new HashMap<>();
        public Map<String, Double> motorPowers = new HashMap<>();
        public TrajectoryPoint(int stepNumber, long timestamp) {
            this.stepNumber = stepNumber; this.timestamp = timestamp;
        }
    }

    public static class MotorMovement {
        public int startPosition;
        public int endPosition;
        public double power;
        public long startTime;
        public long endTime;
        public boolean isPositiveDirection;

        public MotorMovement(int startPosition, int endPosition, double power,
                             long startTime, long endTime, boolean isPositiveDirection) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.power = power;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isPositiveDirection = isPositiveDirection;
        }
    }

    private LinearOpMode opMode;
    private Map<String, MotorConfig> motorConfigs = new HashMap<>();
    private List<TrajectoryPoint> trajectory = new CopyOnWriteArrayList<>();
    private ElapsedTime recordingTimer = new ElapsedTime();
    private volatile boolean isRecording = false;
    private int currentStep = 0;
    private Thread recordingThread;
    private GTRWebServer webServer;
    private String serverIpAddress = "192.168.43.1";

    private static final int RECORDING_INTERVAL_MS = 50;
    private static final int WEB_SERVER_PORT = 8088;
    private static final int DIRECTION_CHANGE_THRESHOLD = 5;
    private static final long MIN_MOVEMENT_TIME_MS = 200;

    public GhostTrajectoryRecorder(LinearOpMode opMode) {
        this.opMode = opMode;
        try {
            String detectedIp = getLocalIpAddress();
            if (!detectedIp.equals("не определен")) {
                serverIpAddress = detectedIp;
            }

            webServer = new GTRWebServer(WEB_SERVER_PORT);
            webServer.start();

            opMode.telemetry.addData("GTR", "Веб-сервер запущен");
            opMode.telemetry.addData("GTR", "Адрес: http://%s:%d", serverIpAddress, WEB_SERVER_PORT);
            opMode.telemetry.update();

        } catch (IOException e) {
            opMode.telemetry.addData("GTR Ошибка", e.getMessage());
            opMode.telemetry.update();
        }
    }

    // ОСНОВНЫЕ МЕТОДЫ

    public void addMotor(String name, double speed, boolean reversed) {
        try {
            DcMotorEx motor = opMode.hardwareMap.get(DcMotorEx.class, name);
            motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

            MotorConfig config = new MotorConfig(name, speed, reversed);
            config.motor = motor;
            motorConfigs.put(name, config);

        } catch (Exception e) {
            opMode.telemetry.addData("GTR", "Мотор '%s' не найден", name);
        }
    }

    public void startRecording() {
        if (isRecording) return;

        trajectory.clear();
        currentStep = 0;
        recordingTimer.reset();
        isRecording = true;

        for (MotorConfig config : motorConfigs.values()) {
            if (config.motor != null) {
                config.motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                config.motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            }
        }

        recordingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && isRecording && opMode.opModeIsActive()) {
                recordPoint();
                try { Thread.sleep(RECORDING_INTERVAL_MS); }
                catch (InterruptedException e) { break; }
            }
        });
        recordingThread.start();
    }

    private void recordPoint() {
        TrajectoryPoint point = new TrajectoryPoint(currentStep++, (long) recordingTimer.milliseconds());

        for (MotorConfig config : motorConfigs.values()) {
            if (config.motor != null) {
                point.motorPositions.put(config.name, config.motor.getCurrentPosition());
                point.motorPowers.put(config.name, config.motor.getPower());
            }
        }

        trajectory.add(point);
    }

    public void stopRecording() {
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
            try { recordingThread.join(500); }
            catch (InterruptedException ignored) {}
        }
    }

    // ГЕНЕРАЦИЯ КОДА

    private Map<String, List<MotorMovement>> extractMotorMovements() {
        Map<String, List<MotorMovement>> movements = new HashMap<>();

        if (trajectory.isEmpty()) {
            return movements;
        }

        for (String motorName : motorConfigs.keySet()) {
            movements.put(motorName, new ArrayList<>());
        }

        for (Map.Entry<String, MotorConfig> entry : motorConfigs.entrySet()) {
            String motorName = entry.getKey();
            List<MotorMovement> motorMovements = movements.get(motorName);

            List<MotorPosition> positions = new ArrayList<>();
            for (TrajectoryPoint point : trajectory) {
                Integer pos = point.motorPositions.get(motorName);
                Double power = point.motorPowers.get(motorName);
                if (pos != null) {
                    double actualPower = (power != null && Math.abs(power) > 0.01) ?
                            Math.abs(power) : entry.getValue().speed;
                    positions.add(new MotorPosition(pos, actualPower, point.timestamp));
                }
            }

            if (positions.size() < 2) {
                continue;
            }

            int currentStartIdx = 0;
            int currentStartPos = positions.get(0).position;
            long currentStartTime = positions.get(0).timestamp;
            double currentPower = positions.get(0).power;
            boolean currentDirection = true;

            for (int i = 1; i < positions.size(); i++) {
                MotorPosition current = positions.get(i);
                MotorPosition prev = positions.get(i-1);

                int diff = current.position - prev.position;
                boolean direction = diff > 0;
                long timeDiff = current.timestamp - positions.get(currentStartIdx).timestamp;

                boolean directionChanged = (currentDirection && diff < -DIRECTION_CHANGE_THRESHOLD) ||
                        (!currentDirection && diff > DIRECTION_CHANGE_THRESHOLD);

                boolean enoughTime = timeDiff >= MIN_MOVEMENT_TIME_MS;

                if (directionChanged || (enoughTime && i == positions.size() - 1)) {
                    int endPos;
                    if (currentDirection) {
                        endPos = currentStartPos;
                        for (int j = currentStartIdx; j <= i; j++) {
                            if (positions.get(j).position > endPos) {
                                endPos = positions.get(j).position;
                                currentPower = positions.get(j).power;
                            }
                        }
                    } else {
                        endPos = currentStartPos;
                        for (int j = currentStartIdx; j <= i; j++) {
                            if (positions.get(j).position < endPos) {
                                endPos = positions.get(j).position;
                                currentPower = positions.get(j).power;
                            }
                        }
                    }

                    if (endPos != currentStartPos) {
                        motorMovements.add(new MotorMovement(
                                currentStartPos,
                                endPos,
                                currentPower,
                                currentStartTime,
                                current.timestamp,
                                currentDirection
                        ));
                    }

                    currentStartIdx = i;
                    currentStartPos = positions.get(i).position;
                    currentStartTime = positions.get(i).timestamp;
                    currentDirection = direction;
                    currentPower = positions.get(i).power;
                } else if (Math.abs(diff) > DIRECTION_CHANGE_THRESHOLD) {
                    currentDirection = direction;
                }
            }

            if (currentStartIdx < positions.size() - 1) {
                int lastIdx = positions.size() - 1;
                int endPos = positions.get(lastIdx).position;
                if (endPos != currentStartPos) {
                    motorMovements.add(new MotorMovement(
                            currentStartPos,
                            endPos,
                            currentPower,
                            currentStartTime,
                            positions.get(lastIdx).timestamp,
                            currentDirection
                    ));
                }
            }
        }

        return movements;
    }

    private static class MotorPosition {
        int position;
        double power;
        long timestamp;

        MotorPosition(int position, double power, long timestamp) {
            this.position = position;
            this.power = power;
            this.timestamp = timestamp;
        }
    }

    public String generateJavaCode() {
        if (trajectory.isEmpty()) {
            return "// Нет записанной траектории\n" +
                    "// 1. Добавьте моторы\n" +
                    "// 2. Начните запись\n" +
                    "// 3. Двигайте робота\n" +
                    "// 4. Остановите запись\n";
        }

        Map<String, List<MotorMovement>> allMovements = extractMotorMovements();

        int maxMovements = 0;
        for (List<MotorMovement> movements : allMovements.values()) {
            if (movements.size() > maxMovements) {
                maxMovements = movements.size();
            }
        }

        if (maxMovements > 10) {
            allMovements = simplifyMovements(allMovements, 10);
        }

        StringBuilder code = new StringBuilder();
        code.append("// ====================================================\n");
        code.append("// Автономный режим - Ghost Trajectory Recorder\n");
        code.append("// Сгенерировано: ").append(new Date()).append("\n");
        code.append("// Точки: ").append(trajectory.size()).append("\n");

        int totalMovements = 0;
        for (List<MotorMovement> movements : allMovements.values()) {
            totalMovements += movements.size();
        }
        code.append("// Движений: ").append(totalMovements).append("\n");
        code.append("// ====================================================\n\n");

        code.append("    // Объявление моторов\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("    private DcMotor ").append(motorName).append(";\n");
        }
        code.append("\n");

        code.append("    @Override\n");
        code.append("    public void runOpMode() {\n");
        code.append("        // Инициализация моторов\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("        ").append(motorName)
                    .append(" = hardwareMap.get(DcMotor.class, \"")
                    .append(motorName).append("\");\n");
        }
        code.append("\n");

        code.append("        // Настройка моторов\n");
        for (MotorConfig config : motorConfigs.values()) {
            code.append("        ").append(config.name).append(".setDirection(")
                    .append(config.reversed ? "DcMotor.Direction.REVERSE" : "DcMotor.Direction.FORWARD")
                    .append(");\n");
            code.append("        ").append(config.name).append(".setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);\n");
            code.append("        ").append(config.name).append(".setMode(DcMotor.RunMode.RUN_USING_ENCODER);\n");
            code.append("        ").append(config.name).append(".setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);\n");
        }
        code.append("\n");
        code.append("        waitForStart();\n");
        code.append("\n");

        List<MotorCommand> allCommands = new ArrayList<>();
        boolean firstMovement = true;
        for (Map.Entry<String, List<MotorMovement>> entry : allMovements.entrySet()) {
            String motorName = entry.getKey();
            for (MotorMovement movement : entry.getValue()) {
                allCommands.add(new MotorCommand(motorName, movement, firstMovement));
                firstMovement = false;
            }
        }

        int movementCount = 0;
        for (int i = 0; i < allCommands.size(); i++) {
            MotorCommand cmd = allCommands.get(i);
            movementCount++;

            code.append("        // Движение ").append(movementCount).append(": ")
                    .append(cmd.motorName).append(" -> ").append(cmd.movement.endPosition).append("\n");

            code.append("        ").append(cmd.motorName).append(".setTargetPosition(")
                    .append(cmd.movement.endPosition).append(");\n");

            if (cmd.isFirstMovement) {
                code.append("        ").append(cmd.motorName).append(".setMode(DcMotor.RunMode.RUN_TO_POSITION);\n");
            }

            code.append("        ").append(cmd.motorName).append(".setPower(")
                    .append(String.format("%.2f", cmd.movement.power)).append(");\n");

            code.append("        while (opModeIsActive() && ").append(cmd.motorName).append(".isBusy()) {\n");
            code.append("            sleep(20);\n");
            code.append("        }\n");

            if (i < allCommands.size() - 1) {
                code.append("        sleep(50);\n\n");
            } else {
                code.append("\n");
            }
        }

        code.append("        // Остановка\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("        ").append(motorName).append(".setPower(0);\n");
        }

        code.append("        for (DcMotor motor : new DcMotor[]{");
        boolean first = true;
        for (String motorName : motorConfigs.keySet()) {
            if (!first) code.append(", ");
            code.append(motorName);
            first = false;
        }
        code.append("}) {\n");
        code.append("            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);\n");
        code.append("        }\n");

        code.append("\n        telemetry.addData(\"Status\", \"Complete\");\n");
        code.append("        telemetry.update();\n");
        code.append("    }\n");

        return code.toString();
    }

    private static class MotorCommand {
        String motorName;
        MotorMovement movement;
        boolean isFirstMovement;

        MotorCommand(String motorName, MotorMovement movement, boolean isFirstMovement) {
            this.motorName = motorName;
            this.movement = movement;
            this.isFirstMovement = isFirstMovement;
        }
    }

    private Map<String, List<MotorMovement>> simplifyMovements(Map<String, List<MotorMovement>> movements, int maxPerMotor) {
        Map<String, List<MotorMovement>> simplified = new HashMap<>();

        for (Map.Entry<String, List<MotorMovement>> entry : movements.entrySet()) {
            String motorName = entry.getKey();
            List<MotorMovement> motorMovements = entry.getValue();

            if (motorMovements.size() <= maxPerMotor) {
                simplified.put(motorName, motorMovements);
                continue;
            }

            List<MotorMovement> simplifiedList = new ArrayList<>();
            MotorMovement current = motorMovements.get(0);

            for (int i = 1; i < motorMovements.size(); i++) {
                MotorMovement next = motorMovements.get(i);

                if (current.isPositiveDirection == next.isPositiveDirection &&
                        (next.startTime - current.endTime) < 500) {

                    current = new MotorMovement(
                            current.startPosition,
                            next.endPosition,
                            Math.max(current.power, next.power),
                            current.startTime,
                            next.endTime,
                            current.isPositiveDirection
                    );
                } else {
                    simplifiedList.add(current);
                    current = next;
                }

                if (simplifiedList.size() >= maxPerMotor - 1) {
                    break;
                }
            }

            if (simplifiedList.size() < maxPerMotor) {
                simplifiedList.add(current);
            }

            simplified.put(motorName, simplifiedList);
        }

        return simplified;
    }

    public void stopAll() {
        stopRecording();
        if (webServer != null) webServer.stop();
    }

    public String getWebInterfaceUrl() {
        return "http://" + serverIpAddress + ":" + WEB_SERVER_PORT;
    }

    public int getRecordedPointsCount() { return trajectory.size(); }
    public boolean isRecording() { return isRecording; }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.")) {
                        return ip;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "не определен";
    }

    // ВЕБ-СЕРВЕР

    private class GTRWebServer extends fi.iki.elonen.NanoHTTPD {
        public GTRWebServer(int port) { super(port); }

        @Override
        public Response serve(IHTTPSession session) {
            Response response = handleRequest(session);
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type");
            return response;
        }

        private Response handleRequest(IHTTPSession session) {
            String uri = session.getUri();

            try {
                if (uri.startsWith("/api/")) {
                    return handleApiRequest(uri, session.getMethod());
                } else if ("/".equals(uri) || uri.isEmpty()) {
                    return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", getFullWebPage());
                }
            } catch (Exception e) {
                return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "application/json",
                        "{\"error\":\"" + e.getMessage() + "\"}"
                );
            }

            return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "404 Not Found"
            );
        }

        private Response handleApiRequest(String uri, Method method) throws Exception {
            switch (uri) {
                case "/api/startRecording":
                    if (Method.POST.equals(method)) {
                        startRecording();
                        return newFixedLengthResponse(Response.Status.OK, "application/json",
                                "{\"success\":true,\"message\":\"Запись начата\"}");
                    }
                    break;

                case "/api/stopRecording":
                    if (Method.POST.equals(method)) {
                        stopRecording();
                        return newFixedLengthResponse(Response.Status.OK, "application/json",
                                "{\"success\":true,\"message\":\"Запись остановлена\"}");
                    }
                    break;

                case "/api/telemetry":
                    JSONObject telemetry = new JSONObject();
                    telemetry.put("connected", true);
                    telemetry.put("isRecording", isRecording);
                    telemetry.put("pointsRecorded", trajectory.size());
                    telemetry.put("recordingTime", isRecording ? recordingTimer.milliseconds() : 0);
                    telemetry.put("serverTime", System.currentTimeMillis());

                    JSONObject motors = new JSONObject();
                    for (MotorConfig config : motorConfigs.values()) {
                        if (config.motor != null) {
                            JSONObject motorData = new JSONObject();
                            motorData.put("position", config.motor.getCurrentPosition());
                            motorData.put("power", config.motor.getPower());
                            motorData.put("busy", config.motor.isBusy());
                            motorData.put("speed", config.speed);
                            motors.put(config.name, motorData);
                        }
                    }
                    telemetry.put("motors", motors);

                    return newFixedLengthResponse(Response.Status.OK, "application/json", telemetry.toString());

                case "/api/generateCode":
                    String code = generateJavaCode();
                    JSONObject response = new JSONObject();
                    response.put("code", code);
                    return newFixedLengthResponse(Response.Status.OK, "application/json", response.toString());

                case "/api/clear":
                    if (Method.POST.equals(method)) {
                        trajectory.clear();
                        return newFixedLengthResponse(Response.Status.OK, "application/json",
                                "{\"success\":true,\"message\":\"Траектория очищена\"}");
                    }
                    break;

                case "/api/ping":
                    JSONObject ping = new JSONObject();
                    ping.put("status", "ok");
                    ping.put("service", "gtr");
                    ping.put("version", "6.0");
                    return newFixedLengthResponse(Response.Status.OK, "application/json", ping.toString());
            }

            return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    "{\"error\":\"Not found\"}"
            );
        }

        private String getFullWebPage() {
            return "<!DOCTYPE html>" +
                    "<html lang='ru'>" +
                    "<head>" +
                    "    <meta charset='UTF-8'>" +
                    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "    <title>GTR Recorder</title>" +
                    "    <style>" +
                    "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                    "        body { font-family: Arial, Helvetica, sans-serif; font-size: 14px; background: #ffffff; color: #353833; }" +
                    "        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }" +
                    "        .header { padding: 20px 0; border-bottom: 1px solid #d0d9e0; margin-bottom: 20px; }" +
                    "        .header h1 { color: #2c4557; font-size: 20px; margin-bottom: 5px; }" +
                    "        .header .subtitle { color: #4A6782; }" +
                    "        .status-bar { background: #dee3e9; padding: 15px; margin-bottom: 20px; display: flex; align-items: center; gap: 15px; }" +
                    "        .status-indicator { display: flex; align-items: center; gap: 8px; }" +
                    "        .status-dot { width: 10px; height: 10px; border-radius: 50%; background: #ccc; }" +
                    "        .status-dot.connected { background: #2e7d32; }" +
                    "        .stats { display: flex; gap: 20px; }" +
                    "        .stat { text-align: center; }" +
                    "        .stat .label { color: #666; font-size: 12px; }" +
                    "        .stat .value { font-weight: bold; color: #2c4557; }" +
                    "        .main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }" +
                    "        @media (max-width: 800px) { .main-grid { grid-template-columns: 1fr; } }" +
                    "        .panel { border: 1px solid #d0d9e0; padding: 15px; }" +
                    "        .panel h2 { color: #2c4557; font-size: 16px; margin-bottom: 15px; padding-bottom: 8px; border-bottom: 1px solid #d0d9e0; }" +
                    "        .buttons-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-bottom: 15px; }" +
                    "        .btn { padding: 10px; border: 1px solid #4A6782; background: #4A6782; color: white; cursor: pointer; font-size: 14px; }" +
                    "        .btn:hover { background: #5a7892; }" +
                    "        .btn:disabled { background: #cccccc; border-color: #cccccc; cursor: not-allowed; }" +
                    "        .btn-clear { background: #666; border-color: #666; }" +
                    "        .btn-clear:hover { background: #777; }" +
                    "        .btn-generate { background: #2c4557; border-color: #2c4557; }" +
                    "        .btn-generate:hover { background: #3c5567; }" +
                    "        .motors-list { display: grid; gap: 10px; }" +
                    "        .motor-card { border: 1px solid #d0d9e0; padding: 10px; }" +
                    "        .motor-name { font-weight: bold; color: #4A6782; margin-bottom: 8px; }" +
                    "        .motor-data { display: grid; gap: 5px; }" +
                    "        .data-row { display: flex; justify-content: space-between; }" +
                    "        .data-label { color: #666; }" +
                    "        .code-panel { margin-bottom: 20px; }" +
                    "        .code-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }" +
                    "        .code-actions { display: flex; gap: 8px; }" +
                    "        .code-actions .btn { padding: 6px 12px; font-size: 13px; }" +
                    "        .code-output { background: #f8f8f8; border: 1px solid #d0d9e0; padding: 15px; font-family: monospace; font-size: 13px; white-space: pre-wrap; max-height: 400px; overflow-y: auto; }" +
                    "        .footer { padding: 20px 0; border-top: 1px solid #d0d9e0; color: #666; font-size: 12px; text-align: center; }" +
                    "        .notification { position: fixed; bottom: 20px; right: 20px; padding: 12px 20px; background: #2c4557; color: white; border: 1px solid #d0d9e0; display: none; }" +
                    "        .notification.show { display: block; }" +
                    "        ::-webkit-scrollbar { width: 8px; }" +
                    "        ::-webkit-scrollbar-track { background: #f1f1f1; }" +
                    "        ::-webkit-scrollbar-thumb { background: #888; }" +
                    "        ::-webkit-scrollbar-thumb:hover { background: #555; }" +
                    "    </style>" +
                    "</head>" +
                    "<body>" +
                    "    <div class='container'>" +
                    "        <div class='header'>" +
                    "            <h1>Ghost Trajectory Recorder</h1>" +
                    "            <div class='subtitle'>Простой рекордер траекторий для FTC</div>" +
                    "        </div>" +
                    "        " +
                    "        <div class='status-bar'>" +
                    "            <div class='status-indicator'>" +
                    "                <div class='status-dot' id='statusDot'></div>" +
                    "                <span id='statusText'>Подключение...</span>" +
                    "            </div>" +
                    "            <div class='stats'>" +
                    "                <div class='stat'>" +
                    "                    <div class='label'>Точки</div>" +
                    "                    <div class='value' id='pointsCount'>0</div>" +
                    "                </div>" +
                    "                <div class='stat'>" +
                    "                    <div class='label'>Время</div>" +
                    "                    <div class='value' id='recordingTime'>0мс</div>" +
                    "                </div>" +
                    "                <div class='stat'>" +
                    "                    <div class='label'>Статус</div>" +
                    "                    <div class='value' id='recordingStatus'>—</div>" +
                    "                </div>" +
                    "            </div>" +
                    "        </div>" +
                    "        " +
                    "        <div class='main-grid'>" +
                    "            <div class='panel'>" +
                    "                <h2>Управление записью</h2>" +
                    "                <div class='buttons-grid'>" +
                    "                    <button class='btn' id='startBtn' onclick='startRecording()'>Начать запись</button>" +
                    "                    <button class='btn' id='stopBtn' onclick='stopRecording()' disabled>Остановить</button>" +
                    "                    <button class='btn btn-generate' onclick='generateCode()'>Генерация кода</button>" +
                    "                    <button class='btn btn-clear' onclick='clearRecording()'>Очистить</button>" +
                    "                </div>" +
                    "            </div>" +
                    "            " +
                    "            <div class='panel'>" +
                    "                <h2>Моторы</h2>" +
                    "                <div class='motors-list' id='motorsContainer'>" +
                    "                    <div style='color: #666; text-align: center; padding: 20px;'>Нет данных моторов</div>" +
                    "                </div>" +
                    "            </div>" +
                    "        </div>" +
                    "        " +
                    "        <div class='panel code-panel'>" +
                    "            <div class='code-header'>" +
                    "                <h2>Сгенерированный код</h2>" +
                    "                <div class='code-actions'>" +
                    "                    <button class='btn' onclick='copyCode()'>Копировать</button>" +
                    "                    <button class='btn' onclick='downloadCode()'>Скачать</button>" +
                    "                </div>" +
                    "            </div>" +
                    "            <div class='code-output' id='codeOutput'>" +
                    "// Код появится здесь после генерации" +
                    "" +
                    "// 1. Нажмите 'Начать запись'" +
                    "// 2. Управляйте роботом с джойстиков" +
                    "// 3. Нажмите 'Остановить'" +
                    "// 4. Нажмите 'Генерация кода'" +
                    "            </div>" +
                    "        </div>" +
                    "        " +
                    "        <div class='footer'>" +
                    "            GTR v6.0 | Подключено к роботу | <span id='connectionInfo'>IP: загрузка...</span>" +
                    "        </div>" +
                    "    </div>" +
                    "    " +
                    "    <div class='notification' id='notification'></div>" +
                    "    " +
                    "    <script>" +
                    "        const API_BASE = '/api';\n" +
                    "        let telemetryInterval = null;\n" +
                    "        let connectionInterval = null;\n" +
                    "        let isConnected = false;\n" +
                    "\n" +
                    "        async function checkConnection() {\n" +
                    "            try {\n" +
                    "                const response = await fetch(API_BASE + '/ping');\n" +
                    "                if (response.ok) {\n" +
                    "                    updateConnectionStatus(true);\n" +
                    "                    return true;\n" +
                    "                }\n" +
                    "            } catch (error) {\n" +
                    "                console.log('Connection check failed:', error);\n" +
                    "            }\n" +
                    "            updateConnectionStatus(false);\n" +
                    "            return false;\n" +
                    "        }\n" +
                    "\n" +
                    "        function updateConnectionStatus(connected) {\n" +
                    "            const dot = document.getElementById('statusDot');\n" +
                    "            const statusText = document.getElementById('statusText');\n" +
                    "            const connectionInfo = document.getElementById('connectionInfo');\n" +
                    "            \n" +
                    "            if (connected && !isConnected) {\n" +
                    "                dot.className = 'status-dot connected';\n" +
                    "                statusText.textContent = 'Подключено';\n" +
                    "                connectionInfo.textContent = 'IP: ' + window.location.hostname;\n" +
                    "                showNotification('Подключено к роботу');\n" +
                    "                isConnected = true;\n" +
                    "                startTelemetryUpdates();\n" +
                    "            } else if (!connected && isConnected) {\n" +
                    "                dot.className = 'status-dot';\n" +
                    "                statusText.textContent = 'Отключено';\n" +
                    "                connectionInfo.textContent = 'Нет связи';\n" +
                    "                isConnected = false;\n" +
                    "                stopTelemetryUpdates();\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function startTelemetryUpdates() {\n" +
                    "            if (telemetryInterval) clearInterval(telemetryInterval);\n" +
                    "            telemetryInterval = setInterval(fetchTelemetry, 1000);\n" +
                    "            fetchTelemetry();\n" +
                    "        }\n" +
                    "\n" +
                    "        function stopTelemetryUpdates() {\n" +
                    "            if (telemetryInterval) {\n" +
                    "                clearInterval(telemetryInterval);\n" +
                    "                telemetryInterval = null;\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        async function fetchTelemetry() {\n" +
                    "            try {\n" +
                    "                const response = await fetch(API_BASE + '/telemetry');\n" +
                    "                if (!response.ok) throw new Error('Network error');\n" +
                    "                \n" +
                    "                const data = await response.json();\n" +
                    "                updateTelemetryDisplay(data);\n" +
                    "                updateControls(data);\n" +
                    "                \n" +
                    "            } catch (error) {\n" +
                    "                console.error('Telemetry error:', error);\n" +
                    "                updateConnectionStatus(false);\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function updateTelemetryDisplay(data) {\n" +
                    "            document.getElementById('pointsCount').textContent = data.pointsRecorded || 0;\n" +
                    "            document.getElementById('recordingTime').textContent = Math.round(data.recordingTime || 0) + 'мс';\n" +
                    "            document.getElementById('recordingStatus').textContent = \n" +
                    "                data.isRecording ? 'Запись...' : 'Готово';\n" +
                    "            \n" +
                    "            updateMotorsDisplay(data.motors);\n" +
                    "        }\n" +
                    "\n" +
                    "        function updateMotorsDisplay(motors) {\n" +
                    "            const container = document.getElementById('motorsContainer');\n" +
                    "            \n" +
                    "            if (!motors || Object.keys(motors).length === 0) {\n" +
                    "                container.innerHTML = '<div style=\"color: #666; text-align: center; padding: 20px;\">Нет данных моторов</div>';\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            \n" +
                    "            let html = '';\n" +
                    "            Object.entries(motors).forEach(([name, motorData]) => {\n" +
                    "                html += `\n" +
                    "                    <div class=\"motor-card\">\n" +
                    "                        <div class=\"motor-name\">${name}</div>\n" +
                    "                        <div class=\"motor-data\">\n" +
                    "                            <div class=\"data-row\">\n" +
                    "                                <span class=\"data-label\">Позиция:</span>\n" +
                    "                                <span>${motorData.position}</span>\n" +
                    "                            </div>\n" +
                    "                            <div class=\"data-row\">\n" +
                    "                                <span class=\"data-label\">Мощность:</span>\n" +
                    "                                <span>${motorData.power.toFixed(2)}</span>\n" +
                    "                            </div>\n" +
                    "                            <div class=\"data-row\">\n" +
                    "                                <span class=\"data-label\">Состояние:</span>\n" +
                    "                                <span>${motorData.busy ? 'Работает' : 'Ожидание'}</span>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                `;\n" +
                    "            });\n" +
                    "            \n" +
                    "            container.innerHTML = html;\n" +
                    "        }\n" +
                    "\n" +
                    "        function updateControls(data) {\n" +
                    "            const startBtn = document.getElementById('startBtn');\n" +
                    "            const stopBtn = document.getElementById('stopBtn');\n" +
                    "            \n" +
                    "            if (data.isRecording) {\n" +
                    "                startBtn.disabled = true;\n" +
                    "                stopBtn.disabled = false;\n" +
                    "                startBtn.textContent = 'Запись...';\n" +
                    "            } else {\n" +
                    "                startBtn.disabled = false;\n" +
                    "                stopBtn.disabled = true;\n" +
                    "                startBtn.textContent = 'Начать запись';\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        async function startRecording() {\n" +
                    "            try {\n" +
                    "                const response = await fetch(API_BASE + '/startRecording', { method: 'POST' });\n" +
                    "                const result = await response.json();\n" +
                    "                showNotification(result.message);\n" +
                    "            } catch (error) {\n" +
                    "                showNotification('Ошибка: ' + error.message);\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        async function stopRecording() {\n" +
                    "            try {\n" +
                    "                const response = await fetch(API_BASE + '/stopRecording', { method: 'POST' });\n" +
                    "                const result = await response.json();\n" +
                    "                showNotification(result.message);\n" +
                    "            } catch (error) {\n" +
                    "                showNotification('Ошибка: ' + error.message);\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        async function generateCode() {\n" +
                    "            try {\n" +
                    "                const response = await fetch(API_BASE + '/generateCode');\n" +
                    "                const result = await response.json();\n" +
                    "                document.getElementById('codeOutput').textContent = result.code;\n" +
                    "                showNotification('Код успешно сгенерирован');\n" +
                    "            } catch (error) {\n" +
                    "                showNotification('Ошибка генерации кода');\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        async function copyCode() {\n" +
                    "            const code = document.getElementById('codeOutput').textContent;\n" +
                    "            try {\n" +
                    "                await navigator.clipboard.writeText(code);\n" +
                    "                showNotification('Код скопирован в буфер обмена');\n" +
                    "            } catch (error) {\n" +
                    "                const textarea = document.createElement('textarea');\n" +
                    "                textarea.value = code;\n" +
                    "                document.body.appendChild(textarea);\n" +
                    "                textarea.select();\n" +
                    "                document.execCommand('copy');\n" +
                    "                document.body.removeChild(textarea);\n" +
                    "                showNotification('Код скопирован');\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function downloadCode() {\n" +
                    "            const code = document.getElementById('codeOutput').textContent;\n" +
                    "            const blob = new Blob([code], { type: 'text/plain' });\n" +
                    "            const url = URL.createObjectURL(blob);\n" +
                    "            const a = document.createElement('a');\n" +
                    "            a.href = url;\n" +
                    "            a.download = `gtr_trajectory_${new Date().toISOString().slice(0,10)}.java`;\n" +
                    "            document.body.appendChild(a);\n" +
                    "            a.click();\n" +
                    "            document.body.removeChild(a);\n" +
                    "            URL.revokeObjectURL(url);\n" +
                    "            showNotification('Код скачан');\n" +
                    "        }\n" +
                    "\n" +
                    "        async function clearRecording() {\n" +
                    "            if (confirm('Очистить всю записанную траекторию?')) {\n" +
                    "                try {\n" +
                    "                    const response = await fetch(API_BASE + '/clear', { method: 'POST' });\n" +
                    "                    const result = await response.json();\n" +
                    "                    document.getElementById('codeOutput').textContent = '// Запись очищена\\\\n// Начните новую запись';\n" +
                    "                    showNotification(result.message);\n" +
                    "                    fetchTelemetry();\n" +
                    "                } catch (error) {\n" +
                    "                    showNotification('Ошибка очистки');\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "\n" +
                    "        function showNotification(message) {\n" +
                    "            const notification = document.getElementById('notification');\n" +
                    "            notification.textContent = message;\n" +
                    "            notification.className = 'notification show';\n" +
                    "            \n" +
                    "            setTimeout(() => {\n" +
                    "                notification.className = 'notification';\n" +
                    "            }, 3000);\n" +
                    "        }\n" +
                    "\n" +
                    "        async function init() {\n" +
                    "            connectionInterval = setInterval(checkConnection, 3000);\n" +
                    "            await checkConnection();\n" +
                    "        }\n" +
                    "\n" +
                    "        document.addEventListener('DOMContentLoaded', init);\n" +
                    "        window.addEventListener('beforeunload', () => {\n" +
                    "            if (telemetryInterval) clearInterval(telemetryInterval);\n" +
                    "            if (connectionInterval) clearInterval(connectionInterval);\n" +
                    "        });" +
                    "    </script>" +
                    "</body>" +
                    "</html>";
        }
    }
}