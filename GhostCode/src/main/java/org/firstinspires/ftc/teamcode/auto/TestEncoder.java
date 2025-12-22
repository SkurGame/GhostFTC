package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@Autonomous(name = "TestEncoder_FIXED")
public class TestEncoder extends LinearOpMode {
    private DcMotorEx leftUp;

    @Override
    public void runOpMode() throws InterruptedException {
        leftUp = hardwareMap.get(DcMotorEx.class, "leftUp");

        // 1. СБРОС и НАСТРОЙКА (ВАЖНО!)
        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftUp.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // BRAKE, а не FLOAT!

        // 2. Установите направление ПОСЛЕ сброса
        // Попробуйте ОБА варианта по очереди:
        // leftUp.setDirection(DcMotor.Direction.FORWARD);
        leftUp.setDirection(DcMotor.Direction.REVERSE);

        // 3. Убедитесь, что НЕ используем другие режимы
        // НИКАКИХ RUN_USING_ENCODER или RUN_WITHOUT_ENCODER здесь!

        waitForStart();

        // 4. ТЕСТ: Проверим базовую логику
        telemetry.addLine("Текущая позиция: " + leftUp.getCurrentPosition());
        telemetry.addLine("Задаём цель: +500 тиков");
        telemetry.update();
        sleep(2000);

        // 5. КРИТИЧЕСКИ ВАЖНАЯ последовательность:
        //    АБСОЛЮТНО правильный порядок команд:

        // ШАГ 1: Сначала ЦЕЛЬ (положительная!)
        leftUp.setTargetPosition(500);

        // ШАГ 2: Потом РЕЖИМ
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        // ШАГ 3: И только потом МОЩНОСТЬ (положительная!)
        leftUp.setPower(0.3); // Начните с маленькой мощности 0.3

        telemetry.addLine("\n=== ЗАПУСК RUN_TO_POSITION ===");
        telemetry.addData("Цель", leftUp.getTargetPosition());
        telemetry.addData("Режим", leftUp.getMode());
        telemetry.addData("Мощность", leftUp.getPower());
        telemetry.update();
        sleep(500);

        // 6. МОНИТОРИНГ с детальной информацией
        int lastPosition = leftUp.getCurrentPosition();
        int stallCounter = 0;

        while (opModeIsActive() && leftUp.isBusy()) {
            int currentPos = leftUp.getCurrentPosition();
            int delta = currentPos - lastPosition;

            telemetry.addLine("=== ДЕТАЛЬНАЯ ИНФОРМАЦИЯ ===");
            telemetry.addData("Текущая позиция", currentPos);
            telemetry.addData("Целевая позиция", leftUp.getTargetPosition());
            telemetry.addData("Осталось", leftUp.getTargetPosition() - currentPos);
            telemetry.addData("Мощность", leftUp.getPower());
            telemetry.addData("Изменение (дельта)", delta);
            telemetry.addData("Скорость", leftUp.getVelocity());
            telemetry.addData("isBusy?", leftUp.isBusy());

            // Проверка на "застревание"
            if (Math.abs(delta) < 5) {
                stallCounter++;
                telemetry.addData("⚠️ СТАЛЛ", stallCounter + " циклов");
            } else {
                stallCounter = 0;
            }

            lastPosition = currentPos;
            telemetry.update();

            // Если мотор "застрял" - увеличим мощность
            if (stallCounter > 20 && leftUp.getPower() < 0.7) {
                leftUp.setPower(0.7);
                telemetry.addLine("⚠️ УВЕЛИЧИВАЮ МОЩНОСТЬ ДО 0.7!");
            }

            sleep(50);
        }

        // 7. ПОСЛЕ завершения
        leftUp.setPower(0);
        telemetry.addLine("\n=== ВЫПОЛНЕНО ===");
        telemetry.addData("Финальная позиция", leftUp.getCurrentPosition());
        telemetry.addData("Разница с целью", leftUp.getCurrentPosition() - 500);
        telemetry.update();
        sleep(3000);
    }
}