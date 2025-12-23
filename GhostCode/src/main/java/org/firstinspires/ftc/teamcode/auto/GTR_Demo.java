package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import org.firstinspires.ftc.teamcode.lib.GhostTrajectoryRecorder;

@Autonomous(name = "GTR Demo", group = "GTR")
public class GTR_Demo extends LinearOpMode {

    @Override
    public void runOpMode() {
        // 1. Инициализация GTR
        GhostTrajectoryRecorder gtr = new GhostTrajectoryRecorder(this);

        telemetry.addData("GTR", "Инициализация...");
        telemetry.update();

        // 2. Добавление моторов (имена должны совпадать с конфигурацией в Driver Hub)
        gtr.addMotor("leftUp", 0.7, false);
        gtr.addMotor("leftDown", 0.7, false);


        // 3. Показываем адрес веб-интерфейса
        telemetry.addData("GTR", "Готов к работе!");
        telemetry.addData("GTR", "Откройте в браузере:");
        telemetry.addData("GTR", gtr.getWebInterfaceUrl());
        telemetry.addData("GTR", "Используйте веб-интерфейс для управления");
        telemetry.update();

        waitForStart();

        // 4. Главный цикл просто поддерживает соединение
        while (opModeIsActive()) {
            telemetry.addData("GTR Статус", gtr.isRecording());
            telemetry.addData("Записано точек", gtr.getRecordedPointsCount());
            telemetry.update();
            sleep(1000);
        }

        // 5. Корректное завершение
        gtr.stopAll();
        telemetry.addData("GTR", "Библиотека остановлена");
        telemetry.update();
    }
}