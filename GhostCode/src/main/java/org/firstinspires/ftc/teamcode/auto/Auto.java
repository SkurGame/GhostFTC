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
// Автономный режим - Ghost Trajectory Recorder (GTR)
// Сгенерировано: Tue Dec 23 01:01:19 GMT+03:00 2025
// Точки: 209
// Время: 11136 мс
// ====================================================

    // Объявление моторов (должны быть в hardwareMap)
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

        // --- Группа 1 (точки 1-10) ---
        leftUp.setTargetPosition(0);

        // Перевод моторов в режим следования к позиции
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // Мощности моторов
        // Для мотора leftUp используется скорость по умолчанию: 0.70
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 2 (точки 11-20) ---
        leftUp.setTargetPosition(0);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 3 (точки 21-30) ---
        leftUp.setTargetPosition(-1);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 4 (точки 31-40) ---
        leftUp.setTargetPosition(35);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 5 (точки 41-50) ---
        leftUp.setTargetPosition(170);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 6 (точки 51-60) ---
        leftUp.setTargetPosition(229);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 7 (точки 61-70) ---
        leftUp.setTargetPosition(302);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 8 (точки 71-80) ---
        leftUp.setTargetPosition(419);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 9 (точки 81-90) ---
        leftUp.setTargetPosition(280);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 10 (точки 91-100) ---
        leftUp.setTargetPosition(238);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 11 (точки 101-110) ---
        leftUp.setTargetPosition(110);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 12 (точки 111-120) ---
        leftUp.setTargetPosition(25);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 13 (точки 121-130) ---
        leftUp.setTargetPosition(-46);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 14 (точки 131-140) ---
        leftUp.setTargetPosition(-194);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 15 (точки 141-150) ---
        leftUp.setTargetPosition(-235);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 16 (точки 151-160) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 17 (точки 161-170) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 18 (точки 171-180) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 19 (точки 181-190) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 20 (точки 191-200) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Ожидание завершения движения
        while (opModeIsActive() && leftUp.isBusy()) {
            sleep(20);
        }
        sleep(50); // Пауза между группами

        // --- Группа 21 (точки 201-209) ---
        leftUp.setTargetPosition(-391);

        // Мощности моторов
        leftUp.setPower(0.700);

        // Остановка всех моторов
        leftUp.setPower(0);

        // Возврат в режим использования энкодеров
        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addData("Status", "Complete");
        telemetry.update();
    }

}
