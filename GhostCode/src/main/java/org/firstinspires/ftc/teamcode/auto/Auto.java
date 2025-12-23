package org.firstinspires.ftc.teamcode.auto;

import android.net.http.UrlRequest;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name = "BasicAuto", group = "auto")
public class Auto extends LinearOpMode {
// ====================================================
// Автономный режим - Ghost Trajectory Recorder
// Сгенерировано: Tue Dec 23 16:47:00 GMT+03:00 2025
// Точки: 98
// Движений: 2
// ====================================================

    // Объявление моторов
    private DcMotor leftUp;

    @Override
    public void runOpMode() {
        // Инициализация моторов
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");

        // Настройка моторов
        leftUp.setDirection(DcMotor.Direction.FORWARD);
        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftUp.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        waitForStart();

        // Движение 1: leftUp -> 502
        leftUp.setTargetPosition(502);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.70);
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50);

        // Движение 2: leftUp -> -287
        leftUp.setTargetPosition(-287);
        leftUp.setPower(0.70);
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }

        // Остановка
        leftUp.setPower(0);
        for (DcMotor motor : new DcMotor[]{leftUp}) {
            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }

        telemetry.addData("Status", "Complete");
        telemetry.update();
    }
}
