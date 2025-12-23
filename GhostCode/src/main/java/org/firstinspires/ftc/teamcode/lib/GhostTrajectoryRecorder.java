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

/**
 * Ghost Trajectory Recorder (GTR) v3.2 - исправлена генерация кода
 */
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

    private LinearOpMode opMode;
    private Map<String, MotorConfig> motorConfigs = new HashMap<>();
    private List<TrajectoryPoint> trajectory = new CopyOnWriteArrayList<>();
    private ElapsedTime recordingTimer = new ElapsedTime();
    private volatile boolean isRecording = false;
    private volatile boolean isPlaying = false;
    private int currentStep = 0;
    private Thread recordingThread;
    private GTRWebServer webServer;
    private String serverIpAddress = "192.168.43.1";

    private static final int RECORDING_INTERVAL_MS = 50;
    private static final int WEB_SERVER_PORT = 8088;
    private static final int POINTS_PER_GROUP = 10;

    public GhostTrajectoryRecorder(LinearOpMode opMode) {
        this.opMode = opMode;
        try {
            String detectedIp = getLocalIpAddress();
            if (!detectedIp.equals("не определен")) {
                serverIpAddress = detectedIp;
            }

            webServer = new GTRWebServer(WEB_SERVER_PORT);
            webServer.start();

            opMode.telemetry.addData("GTR", "✓ Веб-сервер запущен");
            opMode.telemetry.addData("GTR", "Адрес: http://%s:%d", serverIpAddress, WEB_SERVER_PORT);
            opMode.telemetry.update();

        } catch (IOException e) {
            opMode.telemetry.addData("GTR Ошибка", e.getMessage());
            opMode.telemetry.update();
        }
    }

    // ============ ОСНОВНЫЕ МЕТОДЫ ============

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

    public void playTrajectory() {
        if (trajectory.isEmpty() || isPlaying) return;

        isPlaying = true;

        new Thread(() -> {
            try {
                for (MotorConfig config : motorConfigs.values()) {
                    if (config.motor != null) {
                        config.motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                        config.motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    }
                }

                for (TrajectoryPoint point : trajectory) {
                    if (!opMode.opModeIsActive()) break;

                    for (Map.Entry<String, Integer> entry : point.motorPositions.entrySet()) {
                        MotorConfig config = motorConfigs.get(entry.getKey());
                        if (config != null && config.motor != null) {
                            config.motor.setTargetPosition(entry.getValue());
                        }
                    }

                    for (MotorConfig config : motorConfigs.values()) {
                        if (config.motor != null) {
                            Double recordedPower = point.motorPowers.get(config.name);
                            double power = (recordedPower != null) ? Math.abs(recordedPower) : config.speed;
                            config.motor.setPower(power);
                        }
                    }

                    boolean motorsBusy = true;
                    while (motorsBusy && opMode.opModeIsActive()) {
                        motorsBusy = false;
                        for (MotorConfig config : motorConfigs.values()) {
                            if (config.motor != null && config.motor.isBusy()) {
                                motorsBusy = true;
                                break;
                            }
                        }
                        if (motorsBusy) Thread.sleep(20);
                    }

                    Thread.sleep(RECORDING_INTERVAL_MS);
                }

                for (MotorConfig config : motorConfigs.values()) {
                    if (config.motor != null) {
                        config.motor.setPower(0);
                        config.motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPlaying = false;
            }
        }).start();
    }

    // ============ ИСПРАВЛЕННАЯ ГЕНЕРАЦИЯ КОДА ============

    public String generateJavaCode() {
        if (trajectory.isEmpty()) {
            return "// Нет записанной траектории\n" +
                    "// 1. Добавьте моторы\n" +
                    "// 2. Начните запись\n" +
                    "// 3. Двигайте робота\n" +
                    "// 4. Остановите запись\n";
        }

        StringBuilder code = new StringBuilder();
        code.append("// ====================================================\n");
        code.append("// Автономный режим - Ghost Trajectory Recorder (GTR)\n");
        code.append("// Сгенерировано: ").append(new Date()).append("\n");
        code.append("// Точки: ").append(trajectory.size()).append("\n");
        code.append("// Время: ").append(trajectory.get(trajectory.size()-1).timestamp).append(" мс\n");
        code.append("// ====================================================\n\n");

        // Генерируем объявления моторов
        code.append("    // Объявление моторов (должны быть в hardwareMap)\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("    private DcMotorEx ").append(motorName).append(";\n");
        }
        code.append("\n");

        // Генерируем код инициализации
        code.append("    @Override\n");
        code.append("    public void runOpMode() {\n");
        code.append("        // Инициализация моторов\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("        ").append(motorName)
                    .append(" = hardwareMap.get(DcMotorEx.class, \"")
                    .append(motorName).append("\");\n");
        }
        code.append("\n");

        // Настройка моторов ДО waitForStart()
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

        // Группируем точки для оптимизации
        int groupCount = (trajectory.size() + POINTS_PER_GROUP - 1) / POINTS_PER_GROUP;

        // Определяем, была ли записана хотя бы одна ненулевая мощность
        boolean hasNonZeroPower = false;
        for (TrajectoryPoint point : trajectory) {
            for (Double power : point.motorPowers.values()) {
                if (power != null && Math.abs(power) > 0.001) {
                    hasNonZeroPower = true;
                    break;
                }
            }
            if (hasNonZeroPower) break;
        }

        for (int group = 0; group < groupCount; group++) {
            int startIdx = group * POINTS_PER_GROUP;
            int endIdx = Math.min(startIdx + POINTS_PER_GROUP, trajectory.size());
            TrajectoryPoint firstPoint = trajectory.get(startIdx);

            code.append("        // --- Группа ").append(group + 1)
                    .append(" (точки ").append(startIdx + 1).append("-").append(endIdx).append(") ---\n");

            // Установка целевых позиций ДО перехода в RUN_TO_POSITION
            for (Map.Entry<String, Integer> entry : firstPoint.motorPositions.entrySet()) {
                code.append("        ").append(entry.getKey())
                        .append(".setTargetPosition(").append(entry.getValue()).append(");\n");
            }

            // Переход в RUN_TO_POSITION только для первой группы
            if (group == 0) {
                code.append("\n        // Перевод моторов в режим следования к позиции\n");
                for (String motorName : motorConfigs.keySet()) {
                    code.append("        ").append(motorName).append(".setMode(DcMotor.RunMode.RUN_TO_POSITION);\n");
                }
            }

            // Установка мощностей - ВАЖНО: мощность должна быть установлена!
            code.append("\n        // Мощности моторов\n");
            for (MotorConfig config : motorConfigs.values()) {
                Double recordedPower = firstPoint.motorPowers.get(config.name);
                double powerToSet;

                if (hasNonZeroPower && recordedPower != null && Math.abs(recordedPower) > 0.001) {
                    // Используем записанную мощность
                    powerToSet = Math.abs(recordedPower);
                } else {
                    // Используем скорость из конфига или стандартную 0.5
                    powerToSet = config.speed > 0 ? config.speed : 0.5;
                    if (group == 0) {
                        code.append("        // Для мотора ").append(config.name)
                                .append(" используется скорость по умолчанию: ").append(String.format("%.2f", powerToSet)).append("\n");
                    }
                }

                code.append("        ").append(config.name)
                        .append(".setPower(").append(String.format("%.3f", powerToSet)).append(");\n");
            }

            // Ожидание выполнения (только если не последняя группа)
            if (group < groupCount - 1) {
                code.append("\n        // Ожидание завершения движения\n");
                code.append("        while (opModeIsActive() && ");

                boolean first = true;
                for (String motorName : motorConfigs.keySet()) {
                    if (!first) code.append(" && ");
                    code.append(motorName).append(".isBusy()");
                    first = false;
                }
                code.append(") {\n");
                code.append("            sleep(20);\n");
                code.append("        }\n");
                code.append("        sleep(50); // Пауза между группами\n\n");
            }
        }

        // Завершение
        code.append("\n        // Остановка всех моторов\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("        ").append(motorName).append(".setPower(0);\n");
        }

        code.append("\n        // Возврат в режим использования энкодеров\n");
        for (String motorName : motorConfigs.keySet()) {
            code.append("        ").append(motorName).append(".setMode(DcMotor.RunMode.RUN_USING_ENCODER);\n");
        }

        code.append("\n        telemetry.addData(\"Status\", \"Complete\");\n");
        code.append("        telemetry.update();\n");
        code.append("    }\n");

        return code.toString();
    }

    public void stopAll() {
        stopRecording();
        isPlaying = false;
        if (webServer != null) webServer.stop();
    }

    public String getWebInterfaceUrl() {
        return "http://" + serverIpAddress + ":" + WEB_SERVER_PORT;
    }

    public int getRecordedPointsCount() { return trajectory.size(); }
    public boolean isRecording() { return isRecording; }
    public boolean isPlaying() { return isPlaying; }

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

    // ============ ВЕБ-СЕРВЕР С ВСТРОЕННЫМИ CSS И JS ============

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

                case "/api/playTrajectory":
                    if (Method.POST.equals(method)) {
                        playTrajectory();
                        return newFixedLengthResponse(Response.Status.OK, "application/json",
                                "{\"success\":true,\"message\":\"Воспроизведение начато\"}");
                    }
                    break;

                case "/api/telemetry":
                    JSONObject telemetry = new JSONObject();
                    telemetry.put("connected", true);
                    telemetry.put("isRecording", isRecording);
                    telemetry.put("isPlaying", isPlaying);
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
                    ping.put("version", "3.2");
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
                    "    <title>Ghost Trajectory Recorder v3.1</title>" +
                    "    <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>" +
                    "    <link href='https://fonts.googleapis.com/css2?family=Roboto+Mono:wght@400;500&family=Roboto:wght@300;400;500;700&display=swap' rel='stylesheet'>" +
                    "    <style>" +
                    getCssStyles() +
                    "    </style>" +
                    "</head>" +
                    "<body>" +
                    "    <div class='app-container'>" +
                    "        <!-- Header -->" +
                    "        <header class='app-header'>" +
                    "            <div class='header-left'>" +
                    "                <div class='logo'>" +
                    "                    <i class='fas fa-ghost'></i>" +
                    "                    <h1>Ghost Trajectory Recorder</h1>" +
                    "                </div>" +
                    "                <div class='version'>v3.1</div>" +
                    "            </div>" +
                    "            <div class='header-right'>" +
                    "                <div class='connection-status' id='connectionStatus'>" +
                    "                    <span class='status-indicator' id='statusIndicator'></span>" +
                    "                    <span id='statusText'>Подключение...</span>" +
                    "                </div>" +
                    "            </div>" +
                    "        </header>" +
                    "        " +
                    "        <!-- Main Content -->" +
                    "        <div class='main-content'>" +
                    "            <!-- Left Column: Controls & Telemetry -->" +
                    "            <div class='left-column'>" +
                    "                <!-- Recording Controls -->" +
                    "                <div class='card control-card'>" +
                    "                    <h2><i class='fas fa-gamepad'></i> Управление записью</h2>" +
                    "                    <div class='stats-row'>" +
                    "                        <div class='stat-box'>" +
                    "                            <div class='stat-label'>Точек</div>" +
                    "                            <div class='stat-value' id='pointsCount'>0</div>" +
                    "                        </div>" +
                    "                        <div class='stat-box'>" +
                    "                            <div class='stat-label'>Время</div>" +
                    "                            <div class='stat-value' id='recordingTime'>0 мс</div>" +
                    "                        </div>" +
                    "                        <div class='stat-box'>" +
                    "                            <div class='stat-label'>Статус</div>" +
                    "                            <div class='stat-value' id='recordingStatus'>Ожидание</div>" +
                    "                        </div>" +
                    "                    </div>" +
                    "                    <div class='controls-grid'>" +
                    "                        <button class='control-btn record-btn' id='startBtn' onclick='startRecording()'>" +
                    "                            <i class='fas fa-circle'></i> Начать запись" +
                    "                        </button>" +
                    "                        <button class='control-btn stop-btn' id='stopBtn' onclick='stopRecording()' disabled>" +
                    "                            <i class='fas fa-square'></i> Остановить" +
                    "                        </button>" +
                    "                        <button class='control-btn play-btn' id='playBtn' onclick='playTrajectory()' disabled>" +
                    "                            <i class='fas fa-play'></i> Воспроизвести" +
                    "                        </button>" +
                    "                        <button class='control-btn clear-btn' onclick='clearRecording()'>" +
                    "                            <i class='fas fa-trash'></i> Очистить" +
                    "                        </button>" +
                    "                    </div>" +
                    "                </div>" +
                    "                " +
                    "                <!-- Motor Telemetry -->" +
                    "                <div class='card telemetry-card'>" +
                    "                    <h2><i class='fas fa-tachometer-alt'></i> Телеметрия моторов</h2>" +
                    "                    <div class='telemetry-container' id='telemetryContainer'>" +
                    "                        <div class='no-data'>Нет данных моторов</div>" +
                    "                    </div>" +
                    "                </div>" +
                    "            </div>" +
                    "            " +
                    "            <!-- Right Column: Code Output -->" +
                    "            <div class='right-column'>" +
                    "                <div class='card code-card'>" +
                    "                    <div class='code-header'>" +
                    "                        <h2><i class='fas fa-code'></i> Сгенерированный код</h2>" +
                    "                        <div class='code-actions'>" +
                    "                            <button class='action-btn' onclick='generateCode()'>" +
                    "                                <i class='fas fa-sync'></i> Обновить" +
                    "                            </button>" +
                    "                            <button class='action-btn' onclick='copyCode()'>" +
                    "                                <i class='fas fa-copy'></i> Копировать" +
                    "                            </button>" +
                    "                            <button class='action-btn' onclick='downloadCode()'>" +
                    "                                <i class='fas fa-download'></i> Скачать" +
                    "                            </button>" +
                    "                        </div>" +
                    "                    </div>" +
                    "                    <div class='code-wrapper'>" +
                    "                        <pre><code id='codeOutput' class='java'>// Код появится здесь после генерации\n\n// 1. Нажмите 'Начать запись'\n// 2. Катайте робота по траектории\n// 3. Нажмите 'Остановить'\n// 4. Нажмите 'Обновить' для генерации кода</code></pre>" +
                    "                    </div>" +
                    "                    <div class='code-info'>" +
                    "                        <i class='fas fa-info-circle'></i>" +
                    "                        <span>Скопируйте этот код в ваш автономный режим</span>" +
                    "                    </div>" +
                    "                </div>" +
                    "            </div>" +
                    "        </div>" +
                    "        " +
                    "        <!-- Footer -->" +
                    "        <footer class='app-footer'>" +
                    "            <div class='footer-content'>" +
                    "                <div class='connection-info'>" +
                    "                    <i class='fas fa-wifi'></i>" +
                    "                    <span id='connectionInfo'>Подключение к роботу...</span>" +
                    "                </div>" +
                    "                <div class='server-info'>" +
                    "                    <span>GTR v3.1 • FTC Robotics</span>" +
                    "                </div>" +
                    "            </div>" +
                    "        </footer>" +
                    "    </div>" +
                    "    " +
                    "    <!-- Notification Toast -->" +
                    "    <div id='notification' class='notification-toast'></div>" +
                    "    " +
                    "    <script>" +
                    getJavaScript() +
                    "    </script>" +
                    "</body>" +
                    "</html>";
        }

        private String getCssStyles() {
            return ":root {" +
                    "    --primary-color: #6366f1;" +
                    "    --primary-dark: #4f46e5;" +
                    "    --success-color: #10b981;" +
                    "    --danger-color: #ef4444;" +
                    "    --warning-color: #f59e0b;" +
                    "    --info-color: #3b82f6;" +
                    "    --dark-bg: #0f172a;" +
                    "    --card-bg: #1e293b;" +
                    "    --card-border: #334155;" +
                    "    --text-primary: #f1f5f9;" +
                    "    --text-secondary: #94a3b8;" +
                    "    --text-code: #e2e8f0;" +
                    "    --code-bg: #0f172a;" +
                    "    --hover-bg: #2d3748;" +
                    "}" +
                    "" +
                    "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                    "" +
                    "body {" +
                    "    font-family: 'Roboto', sans-serif;" +
                    "    background: var(--dark-bg);" +
                    "    color: var(--text-primary);" +
                    "    line-height: 1.6;" +
                    "    min-height: 100vh;" +
                    "    padding: 0;" +
                    "}" +
                    "" +
                    ".app-container {" +
                    "    max-width: 1600px;" +
                    "    margin: 0 auto;" +
                    "    min-height: 100vh;" +
                    "    display: flex;" +
                    "    flex-direction: column;" +
                    "}" +
                    "" +
                    "/* Header */" +
                    ".app-header {" +
                    "    background: var(--card-bg);" +
                    "    border-bottom: 1px solid var(--card-border);" +
                    "    padding: 1rem 2rem;" +
                    "    display: flex;" +
                    "    justify-content: space-between;" +
                    "    align-items: center;" +
                    "}" +
                    "" +
                    ".header-left {" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 1rem;" +
                    "}" +
                    "" +
                    ".logo {" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.75rem;" +
                    "}" +
                    "" +
                    ".logo i {" +
                    "    font-size: 2rem;" +
                    "    color: var(--primary-color);" +
                    "}" +
                    "" +
                    ".logo h1 {" +
                    "    font-size: 1.5rem;" +
                    "    font-weight: 600;" +
                    "    color: var(--text-primary);" +
                    "}" +
                    "" +
                    ".version {" +
                    "    background: var(--primary-color);" +
                    "    color: white;" +
                    "    padding: 0.25rem 0.75rem;" +
                    "    border-radius: 1rem;" +
                    "    font-size: 0.875rem;" +
                    "    font-weight: 500;" +
                    "}" +
                    "" +
                    ".connection-status {" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "    padding: 0.5rem 1rem;" +
                    "    background: rgba(255, 255, 255, 0.05);" +
                    "    border-radius: 0.5rem;" +
                    "}" +
                    "" +
                    ".status-indicator {" +
                    "    width: 10px;" +
                    "    height: 10px;" +
                    "    border-radius: 50%;" +
                    "    background: #6b7280;" +
                    "}" +
                    "" +
                    ".status-indicator.connected {" +
                    "    background: var(--success-color);" +
                    "    box-shadow: 0 0 10px var(--success-color);" +
                    "    animation: pulse 2s infinite;" +
                    "}" +
                    "" +
                    "@keyframes pulse {" +
                    "    0% { opacity: 1; }" +
                    "    50% { opacity: 0.5; }" +
                    "    100% { opacity: 1; }" +
                    "}" +
                    "" +
                    "/* Main Content */" +
                    ".main-content {" +
                    "    display: grid;" +
                    "    grid-template-columns: 1fr 1.5fr;" +
                    "    gap: 1.5rem;" +
                    "    padding: 1.5rem;" +
                    "    flex: 1;" +
                    "}" +
                    "" +
                    "@media (max-width: 1200px) {" +
                    "    .main-content {" +
                    "        grid-template-columns: 1fr;" +
                    "    }" +
                    "}" +
                    "" +
                    "/* Cards */" +
                    ".card {" +
                    "    background: var(--card-bg);" +
                    "    border: 1px solid var(--card-border);" +
                    "    border-radius: 1rem;" +
                    "    padding: 1.5rem;" +
                    "    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);" +
                    "}" +
                    "" +
                    ".card h2 {" +
                    "    font-size: 1.25rem;" +
                    "    font-weight: 600;" +
                    "    color: var(--text-primary);" +
                    "    margin-bottom: 1.25rem;" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "}" +
                    "" +
                    "/* Control Card */" +
                    ".control-card { margin-bottom: 1.5rem; }" +
                    "" +
                    ".stats-row {" +
                    "    display: grid;" +
                    "    grid-template-columns: repeat(3, 1fr);" +
                    "    gap: 1rem;" +
                    "    margin-bottom: 1.5rem;" +
                    "}" +
                    "" +
                    ".stat-box {" +
                    "    background: rgba(255, 255, 255, 0.05);" +
                    "    border-radius: 0.75rem;" +
                    "    padding: 1rem;" +
                    "    text-align: center;" +
                    "}" +
                    "" +
                    ".stat-label {" +
                    "    font-size: 0.875rem;" +
                    "    color: var(--text-secondary);" +
                    "    margin-bottom: 0.5rem;" +
                    "}" +
                    "" +
                    ".stat-value {" +
                    "    font-size: 1.5rem;" +
                    "    font-weight: 700;" +
                    "    font-family: 'Roboto Mono', monospace;" +
                    "}" +
                    "" +
                    ".controls-grid {" +
                    "    display: grid;" +
                    "    grid-template-columns: repeat(2, 1fr);" +
                    "    gap: 1rem;" +
                    "}" +
                    "" +
                    ".control-btn {" +
                    "    padding: 1rem;" +
                    "    border: none;" +
                    "    border-radius: 0.75rem;" +
                    "    font-size: 1rem;" +
                    "    font-weight: 600;" +
                    "    cursor: pointer;" +
                    "    transition: all 0.2s;" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    justify-content: center;" +
                    "    gap: 0.5rem;" +
                    "}" +
                    "" +
                    ".control-btn:hover {" +
                    "    transform: translateY(-2px);" +
                    "    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);" +
                    "}" +
                    "" +
                    ".control-btn:disabled {" +
                    "    opacity: 0.5;" +
                    "    cursor: not-allowed;" +
                    "    transform: none !important;" +
                    "}" +
                    "" +
                    ".record-btn { background: var(--danger-color); color: white; }" +
                    ".stop-btn { background: var(--warning-color); color: white; }" +
                    ".play-btn { background: var(--success-color); color: white; }" +
                    ".clear-btn { background: var(--info-color); color: white; }" +
                    "" +
                    "/* Telemetry Card */" +
                    ".telemetry-card { height: 100%; }" +
                    "" +
                    ".telemetry-container {" +
                    "    display: flex;" +
                    "    flex-direction: column;" +
                    "    gap: 0.75rem;" +
                    "    max-height: 400px;" +
                    "    overflow-y: auto;" +
                    "    padding-right: 0.5rem;" +
                    "}" +
                    "" +
                    ".motor-item {" +
                    "    background: rgba(255, 255, 255, 0.05);" +
                    "    border-radius: 0.75rem;" +
                    "    padding: 1rem;" +
                    "    border-left: 4px solid var(--primary-color);" +
                    "}" +
                    "" +
                    ".motor-header {" +
                    "    display: flex;" +
                    "    justify-content: space-between;" +
                    "    align-items: center;" +
                    "    margin-bottom: 0.75rem;" +
                    "}" +
                    "" +
                    ".motor-name {" +
                    "    font-weight: 600;" +
                    "    color: var(--primary-color);" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "}" +
                    "" +
                    ".motor-status {" +
                    "    font-size: 0.875rem;" +
                    "    padding: 0.25rem 0.75rem;" +
                    "    border-radius: 1rem;" +
                    "    background: var(--hover-bg);" +
                    "}" +
                    "" +
                    ".motor-data {" +
                    "    display: grid;" +
                    "    grid-template-columns: repeat(2, 1fr);" +
                    "    gap: 0.5rem;" +
                    "}" +
                    "" +
                    ".data-row {" +
                    "    display: flex;" +
                    "    justify-content: space-between;" +
                    "    padding: 0.5rem 0;" +
                    "    border-bottom: 1px solid rgba(255, 255, 255, 0.1);" +
                    "}" +
                    "" +
                    ".data-label { color: var(--text-secondary); font-size: 0.875rem; }" +
                    ".data-value { font-family: 'Roboto Mono', monospace; font-weight: 500; }" +
                    "" +
                    ".no-data {" +
                    "    text-align: center;" +
                    "    padding: 2rem;" +
                    "    color: var(--text-secondary);" +
                    "}" +
                    "" +
                    "/* Code Card */" +
                    ".code-card {" +
                    "    height: 100%;" +
                    "    display: flex;" +
                    "    flex-direction: column;" +
                    "}" +
                    "" +
                    ".code-header {" +
                    "    display: flex;" +
                    "    justify-content: space-between;" +
                    "    align-items: center;" +
                    "    margin-bottom: 1rem;" +
                    "}" +
                    "" +
                    ".code-actions {" +
                    "    display: flex;" +
                    "    gap: 0.5rem;" +
                    "}" +
                    "" +
                    ".action-btn {" +
                    "    padding: 0.5rem 1rem;" +
                    "    background: var(--hover-bg);" +
                    "    border: 1px solid var(--card-border);" +
                    "    border-radius: 0.5rem;" +
                    "    color: var(--text-primary);" +
                    "    cursor: pointer;" +
                    "    font-size: 0.875rem;" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "    transition: background 0.2s;" +
                    "}" +
                    "" +
                    ".action-btn:hover { background: var(--card-border); }" +
                    "" +
                    ".code-wrapper {" +
                    "    flex: 1;" +
                    "    background: var(--code-bg);" +
                    "    border-radius: 0.75rem;" +
                    "    padding: 1.5rem;" +
                    "    overflow-y: auto;" +
                    "    margin-bottom: 1rem;" +
                    "}" +
                    "" +
                    "pre {" +
                    "    margin: 0;" +
                    "    font-family: 'Roboto Mono', monospace;" +
                    "    font-size: 0.875rem;" +
                    "    line-height: 1.5;" +
                    "    white-space: pre-wrap;" +
                    "    word-wrap: break-word;" +
                    "}" +
                    "" +
                    "code.java {" +
                    "    color: var(--text-code);" +
                    "}" +
                    "" +
                    ".code-info {" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "    color: var(--text-secondary);" +
                    "    font-size: 0.875rem;" +
                    "    padding-top: 1rem;" +
                    "    border-top: 1px solid var(--card-border);" +
                    "}" +
                    "" +
                    "/* Footer */" +
                    ".app-footer {" +
                    "    background: var(--card-bg);" +
                    "    border-top: 1px solid var(--card-border);" +
                    "    padding: 1rem 2rem;" +
                    "}" +
                    "" +
                    ".footer-content {" +
                    "    display: flex;" +
                    "    justify-content: space-between;" +
                    "    align-items: center;" +
                    "}" +
                    "" +
                    ".connection-info {" +
                    "    display: flex;" +
                    "    align-items: center;" +
                    "    gap: 0.5rem;" +
                    "    font-size: 0.875rem;" +
                    "    color: var(--text-secondary);" +
                    "}" +
                    "" +
                    ".server-info {" +
                    "    font-size: 0.875rem;" +
                    "    color: var(--text-secondary);" +
                    "}" +
                    "" +
                    "/* Notification */" +
                    ".notification-toast {" +
                    "    position: fixed;" +
                    "    bottom: 2rem;" +
                    "    right: 2rem;" +
                    "    padding: 1rem 1.5rem;" +
                    "    border-radius: 0.75rem;" +
                    "    color: white;" +
                    "    font-weight: 500;" +
                    "    z-index: 1000;" +
                    "    opacity: 0;" +
                    "    transform: translateY(100%);" +
                    "    transition: all 0.3s;" +
                    "    max-width: 300px;" +
                    "}" +
                    "" +
                    ".notification-toast.show {" +
                    "    opacity: 1;" +
                    "    transform: translateY(0);" +
                    "}" +
                    "" +
                    ".notification-toast.success { background: var(--success-color); }" +
                    ".notification-toast.error { background: var(--danger-color); }" +
                    ".notification-toast.info { background: var(--info-color); }" +
                    "" +
                    "/* Scrollbar */" +
                    "::-webkit-scrollbar {" +
                    "    width: 8px;" +
                    "    height: 8px;" +
                    "}" +
                    "" +
                    "::-webkit-scrollbar-track {" +
                    "    background: rgba(255, 255, 255, 0.05);" +
                    "    border-radius: 4px;" +
                    "}" +
                    "" +
                    "::-webkit-scrollbar-thumb {" +
                    "    background: var(--primary-color);" +
                    "    border-radius: 4px;" +
                    "}" +
                    "" +
                    "::-webkit-scrollbar-thumb:hover {" +
                    "    background: var(--primary-dark);" +
                    "}";
        }

        private String getJavaScript() {
            return "const API_BASE = '/api';\n" +
                    "let telemetryInterval = null;\n" +
                    "let connectionInterval = null;\n" +
                    "let isConnected = false;\n" +
                    "\n" +
                    "// Проверка соединения\n" +
                    "async function checkConnection() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/ping', {\n" +
                    "            method: 'GET',\n" +
                    "            headers: { 'Accept': 'application/json' }\n" +
                    "        });\n" +
                    "        \n" +
                    "        if (response.ok) {\n" +
                    "            const data = await response.json();\n" +
                    "            updateConnectionStatus(true);\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "    } catch (error) {\n" +
                    "        console.log('Connection check failed:', error);\n" +
                    "    }\n" +
                    "    \n" +
                    "    updateConnectionStatus(false);\n" +
                    "    return false;\n" +
                    "}\n" +
                    "\n" +
                    "// Обновление статуса подключения\n" +
                    "function updateConnectionStatus(connected) {\n" +
                    "    const indicator = document.getElementById('statusIndicator');\n" +
                    "    const statusText = document.getElementById('statusText');\n" +
                    "    const connectionInfo = document.getElementById('connectionInfo');\n" +
                    "    \n" +
                    "    if (connected && !isConnected) {\n" +
                    "        indicator.className = 'status-indicator connected';\n" +
                    "        statusText.textContent = 'Подключено';\n" +
                    "        connectionInfo.textContent = 'Связь установлена • Данные обновляются';\n" +
                    "        showNotification('Подключено к роботу', 'success');\n" +
                    "        isConnected = true;\n" +
                    "        startTelemetryUpdates();\n" +
                    "    } else if (!connected && isConnected) {\n" +
                    "        indicator.className = 'status-indicator';\n" +
                    "        statusText.textContent = 'Отключено';\n" +
                    "        connectionInfo.textContent = 'Нет связи с роботом';\n" +
                    "        isConnected = false;\n" +
                    "        stopTelemetryUpdates();\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// Запуск обновления телеметрии\n" +
                    "function startTelemetryUpdates() {\n" +
                    "    if (telemetryInterval) clearInterval(telemetryInterval);\n" +
                    "    telemetryInterval = setInterval(fetchTelemetry, 1000);\n" +
                    "    fetchTelemetry();\n" +
                    "}\n" +
                    "\n" +
                    "// Остановка обновления телеметрии\n" +
                    "function stopTelemetryUpdates() {\n" +
                    "    if (telemetryInterval) {\n" +
                    "        clearInterval(telemetryInterval);\n" +
                    "        telemetryInterval = null;\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// Получение телеметрии\n" +
                    "async function fetchTelemetry() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/telemetry');\n" +
                    "        if (!response.ok) throw new Error('Network error');\n" +
                    "        \n" +
                    "        const data = await response.json();\n" +
                    "        updateTelemetryDisplay(data);\n" +
                    "        updateControls(data);\n" +
                    "        \n" +
                    "    } catch (error) {\n" +
                    "        console.error('Telemetry error:', error);\n" +
                    "        updateConnectionStatus(false);\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// Обновление отображения телеметрии\n" +
                    "function updateTelemetryDisplay(data) {\n" +
                    "    // Обновление статистики\n" +
                    "    document.getElementById('pointsCount').textContent = data.pointsRecorded || 0;\n" +
                    "    document.getElementById('recordingTime').textContent = Math.round(data.recordingTime || 0) + ' мс';\n" +
                    "    document.getElementById('recordingStatus').textContent = \n" +
                    "        data.isRecording ? 'Запись...' : \n" +
                    "        data.isPlaying ? 'Воспроизведение' : 'Ожидание';\n" +
                    "    \n" +
                    "    // Обновление данных моторов\n" +
                    "    updateMotorsDisplay(data.motors);\n" +
                    "}\n" +
                    "\n" +
                    "// Обновление отображения моторов\n" +
                    "function updateMotorsDisplay(motors) {\n" +
                    "    const container = document.getElementById('telemetryContainer');\n" +
                    "    \n" +
                    "    if (!motors || Object.keys(motors).length === 0) {\n" +
                    "        container.innerHTML = '<div class=\"no-data\">Моторы не добавлены в код</div>';\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    \n" +
                    "    let html = '';\n" +
                    "    Object.entries(motors).forEach(([name, motorData]) => {\n" +
                    "        html += `\n" +
                    "            <div class=\"motor-item\">\n" +
                    "                <div class=\"motor-header\">\n" +
                    "                    <div class=\"motor-name\">\n" +
                    "                        <i class=\"fas fa-motorcycle\"></i>\n" +
                    "                        ${name}\n" +
                    "                    </div>\n" +
                    "                    <div class=\"motor-status\">\n" +
                    "                        ${motorData.busy ? '🟢 Работает' : '⚫ Ожидание'}\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "                <div class=\"motor-data\">\n" +
                    "                    <div class=\"data-row\">\n" +
                    "                        <span class=\"data-label\">Позиция:</span>\n" +
                    "                        <span class=\"data-value\">${motorData.position}</span>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"data-row\">\n" +
                    "                        <span class=\"data-label\">Мощность:</span>\n" +
                    "                        <span class=\"data-value\">${motorData.power.toFixed(3)}</span>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"data-row\">\n" +
                    "                        <span class=\"data-label\">Скорость:</span>\n" +
                    "                        <span class=\"data-value\">${motorData.speed.toFixed(2)}</span>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        `;\n" +
                    "    });\n" +
                    "    \n" +
                    "    container.innerHTML = html;\n" +
                    "}\n" +
                    "\n" +
                    "// Обновление состояния кнопок\n" +
                    "function updateControls(data) {\n" +
                    "    const startBtn = document.getElementById('startBtn');\n" +
                    "    const stopBtn = document.getElementById('stopBtn');\n" +
                    "    const playBtn = document.getElementById('playBtn');\n" +
                    "    \n" +
                    "    if (data.isRecording) {\n" +
                    "        startBtn.disabled = true;\n" +
                    "        stopBtn.disabled = false;\n" +
                    "        playBtn.disabled = true;\n" +
                    "        startBtn.innerHTML = '<i class=\"fas fa-circle\"></i> Запись...';\n" +
                    "    } else if (data.isPlaying) {\n" +
                    "        startBtn.disabled = true;\n" +
                    "        stopBtn.disabled = true;\n" +
                    "        playBtn.disabled = true;\n" +
                    "        playBtn.innerHTML = '<i class=\"fas fa-play\"></i> Воспроизведение...';\n" +
                    "    } else {\n" +
                    "        startBtn.disabled = false;\n" +
                    "        stopBtn.disabled = true;\n" +
                    "        playBtn.disabled = (data.pointsRecorded || 0) === 0;\n" +
                    "        startBtn.innerHTML = '<i class=\"fas fa-circle\"></i> Начать запись';\n" +
                    "        playBtn.innerHTML = '<i class=\"fas fa-play\"></i> Воспроизвести';\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// Управление записью\n" +
                    "async function startRecording() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/startRecording', {\n" +
                    "            method: 'POST'\n" +
                    "        });\n" +
                    "        const result = await response.json();\n" +
                    "        showNotification(result.message, 'success');\n" +
                    "    } catch (error) {\n" +
                    "        showNotification('Ошибка: ' + error.message, 'error');\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "async function stopRecording() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/stopRecording', {\n" +
                    "            method: 'POST'\n" +
                    "        });\n" +
                    "        const result = await response.json();\n" +
                    "        showNotification(result.message, 'info');\n" +
                    "    } catch (error) {\n" +
                    "        showNotification('Ошибка: ' + error.message, 'error');\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "async function playTrajectory() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/playTrajectory', {\n" +
                    "            method: 'POST'\n" +
                    "        });\n" +
                    "        const result = await response.json();\n" +
                    "        showNotification(result.message, 'success');\n" +
                    "    } catch (error) {\n" +
                    "        showNotification('Ошибка: ' + error.message, 'error');\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "async function generateCode() {\n" +
                    "    try {\n" +
                    "        const response = await fetch(API_BASE + '/generateCode');\n" +
                    "        const result = await response.json();\n" +
                    "        document.getElementById('codeOutput').textContent = result.code;\n" +
                    "        showNotification('Код успешно сгенерирован', 'success');\n" +
                    "    } catch (error) {\n" +
                    "        showNotification('Ошибка генерации кода', 'error');\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "async function copyCode() {\n" +
                    "    const code = document.getElementById('codeOutput').textContent;\n" +
                    "    try {\n" +
                    "        await navigator.clipboard.writeText(code);\n" +
                    "        showNotification('Код скопирован в буфер обмена', 'success');\n" +
                    "    } catch (error) {\n" +
                    "        const textarea = document.createElement('textarea');\n" +
                    "        textarea.value = code;\n" +
                    "        document.body.appendChild(textarea);\n" +
                    "        textarea.select();\n" +
                    "        document.execCommand('copy');\n" +
                    "        document.body.removeChild(textarea);\n" +
                    "        showNotification('Код скопирован', 'success');\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "function downloadCode() {\n" +
                    "    const code = document.getElementById('codeOutput').textContent;\n" +
                    "    const blob = new Blob([code], { type: 'text/plain' });\n" +
                    "    const url = URL.createObjectURL(blob);\n" +
                    "    const a = document.createElement('a');\n" +
                    "    a.href = url;\n" +
                    "    a.download = `gtr_trajectory_${new Date().toISOString().slice(0,10)}.java`;\n" +
                    "    document.body.appendChild(a);\n" +
                    "    a.click();\n" +
                    "    document.body.removeChild(a);\n" +
                    "    URL.revokeObjectURL(url);\n" +
                    "    showNotification('Код скачан', 'success');\n" +
                    "}\n" +
                    "\n" +
                    "async function clearRecording() {\n" +
                    "    if (confirm('Очистить всю записанную траекторию?')) {\n" +
                    "        try {\n" +
                    "            const response = await fetch(API_BASE + '/clear', {\n" +
                    "                method: 'POST'\n" +
                    "            });\n" +
                    "            const result = await response.json();\n" +
                    "            document.getElementById('codeOutput').textContent = '// Запись очищена\\\\n// Начните новую запись';" +
                    "            showNotification(result.message, 'info');\n" +
                    "            fetchTelemetry();\n" +
                    "        } catch (error) {\n" +
                    "            showNotification('Ошибка очистки', 'error');\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// Уведомления\n" +
                    "function showNotification(message, type) {\n" +
                    "    const notification = document.getElementById('notification');\n" +
                    "    notification.textContent = message;\n" +
                    "    notification.className = `notification-toast ${type} show`;\n" +
                    "    \n" +
                    "    setTimeout(() => {\n" +
                    "        notification.className = 'notification-toast';\n" +
                    "    }, 3000);\n" +
                    "}\n" +
                    "\n" +
                    "// Инициализация\n" +
                    "async function init() {\n" +
                    "    // Проверка подключения каждые 3 секунды\n" +
                    "    connectionInterval = setInterval(checkConnection, 3000);\n" +
                    "    await checkConnection();\n" +
                    "}\n" +
                    "\n" +
                    "// Запуск при загрузке\n" +
                    "document.addEventListener('DOMContentLoaded', init);\n" +
                    "\n" +
                    "// Очистка\n" +
                    "window.addEventListener('beforeunload', () => {\n" +
                    "    if (telemetryInterval) clearInterval(telemetryInterval);\n" +
                    "    if (connectionInterval) clearInterval(connectionInterval);\n" +
                    "});";
        }
    }
}