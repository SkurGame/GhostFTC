package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.Config;

@TeleOp(name = "TeleOp")
public class BasicTeleOp extends LinearOpMode {
    private DcMotor leftUp, leftDown, rightUp, rightDown, pytka, strelylo;
    private Servo Talkalo;

    public void runOpMode() {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");
        pytka = hardwareMap.get(DcMotor.class, "pytka");
        strelylo = hardwareMap.get(DcMotor.class, "strelylo");

        Talkalo = hardwareMap.get(Servo.class, "Talkalo");

        rightUp.setDirection(DcMotor.Direction.REVERSE);
        rightDown.setDirection(DcMotor.Direction.REVERSE);


        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            double ry = gamepad1.right_stick_y;
            double rx = gamepad1.right_stick_x;
            boolean rb = gamepad1.right_bumper;

            double deadZone = 0.15;
            double power = 0.6;
            double boostPower = 0.3;

            if (Math.abs(ry) < deadZone) ry = 0;
            if (Math.abs(rx) < deadZone) rx = 0;

            leftUp.setPower(0);
            leftDown.setPower(0);
            rightUp.setPower(0);
            rightDown.setPower(0);

            // СТРЕЛЯЛО КРУТИТЬ
            strelylo.setPower(0.5);

            // ТАЛКАТЬ СЕРВОЙ ШАРИК
            if(gamepad1.y == true){

            }

            // ПЯТКА ПОДЬЁМ
            if (gamepad1.dpad_up == true){
                pytka.setPower(-1.0);
                sleep(500);
                pytka.setPower(0);
            }

            // ПЯТКА НЕ ПОДЬЁМ
            if (gamepad1.dpad_down == true){
                pytka.setPower(0.7);
                sleep(200);
                pytka.setPower(0);
            }

            // ПОВОРОТ
            if (gamepad1.left_trigger > 0.1) {
                if (rb) {
                    leftUp.setPower(power + boostPower);
                    leftDown.setPower(power + boostPower);
                    rightUp.setPower(-power - boostPower);
                    rightDown.setPower(-power - boostPower);
                } else {
                    leftUp.setPower(power);
                    leftDown.setPower(power);
                    rightUp.setPower(-power);
                    rightDown.setPower(-power);
                }
            }
            else if (gamepad1.right_trigger > 0.1) {
                if (rb) {
                    leftUp.setPower(-power - boostPower);
                    leftDown.setPower(-power - boostPower);
                    rightUp.setPower(power + boostPower);
                    rightDown.setPower(power + boostPower);
                } else {
                    leftUp.setPower(-power);
                    leftDown.setPower(-power);
                    rightUp.setPower(power);
                    rightDown.setPower(power);
                }
            }

            // ДВИЖЕНИЕ ВПЕРЕД
            else if (ry > 0 && Math.abs(rx) < 0.2) {
                if (rb) {
                    leftUp.setPower(power + boostPower);
                    leftDown.setPower(power + boostPower);
                    rightUp.setPower(power + boostPower);
                    rightDown.setPower(power + boostPower);
                } else {
                    leftUp.setPower(power);
                    leftDown.setPower(power);
                    rightUp.setPower(power);
                    rightDown.setPower(power);
                }
            }

            // ДВИЖЕНИЕ НАЗАД
            else if (ry < 0 && Math.abs(rx) < 0.2) {
                if (rb) {
                    leftUp.setPower(-power - boostPower);
                    leftDown.setPower(-power - boostPower);
                    rightUp.setPower(-power - boostPower);
                    rightDown.setPower(-power - boostPower);
                } else {
                    leftUp.setPower(-power);
                    leftDown.setPower(-power);
                    rightUp.setPower(-power);
                    rightDown.setPower(-power);
                }
            }

            // ДВИЖЕНИЕ ВПРАВО
            else if (rx > 0 && Math.abs(ry) < 0.2) {
                if (rb) {
                    leftUp.setPower(-power - boostPower);
                    leftDown.setPower(power + boostPower);
                    rightUp.setPower(power + boostPower);
                    rightDown.setPower(-power - boostPower);
                } else {
                    leftUp.setPower(-power);
                    leftDown.setPower(power);
                    rightUp.setPower(power);
                    rightDown.setPower(-power);
                }
            }

            // ДВИЖЕНИЕ ВЛЕВО
            else if (rx < 0 && Math.abs(ry) < 0.2) {
                if (rb) {
                    leftUp.setPower(power + boostPower);
                    leftDown.setPower(-power - boostPower);
                    rightUp.setPower(-power - boostPower);
                    rightDown.setPower(power + boostPower);
                } else {
                    leftUp.setPower(power);
                    leftDown.setPower(-power);
                    rightUp.setPower(-power);
                    rightDown.setPower(power);
                }
            }

            // ДИАГОНАЛЬ ВПЕРЕД-ВПРАВО
            else if (ry < 0 && rx > 0) {
                if (rb) {
                    leftUp.setPower(-power - boostPower);
                    leftDown.setPower(0);
                    rightUp.setPower(0);
                    rightDown.setPower(-power - boostPower);
                } else {
                    leftUp.setPower(-power);
                    leftDown.setPower(0);
                    rightUp.setPower(0);
                    rightDown.setPower(-power);
                }
            }

            // ДИАГОНАЛЬ ВПЕРЕД-ВЛЕВО
            else if (ry < 0 && rx < 0) {
                if (rb) {
                    leftUp.setPower(0);
                    leftDown.setPower(-power - boostPower);
                    rightUp.setPower(-power - boostPower);
                    rightDown.setPower(0);
                } else {
                    leftUp.setPower(0);
                    leftDown.setPower(-power);
                    rightUp.setPower(-power);
                    rightDown.setPower(0);
                }
            }

            // ДИАГОНАЛЬ НАЗАД-ВПРАВО
            else if (ry > 0 && rx > 0) {
                if (rb) {
                    leftUp.setPower(0);
                    leftDown.setPower(power + boostPower);
                    rightUp.setPower(power + boostPower);
                    rightDown.setPower(0);
                } else {
                    leftUp.setPower(0);
                    leftDown.setPower(power);
                    rightUp.setPower(power);
                    rightDown.setPower(0);
                }
            }

            // ДИАГОНАЛЬ НАЗАД-ВЛЕВО
            else if (ry > 0 && rx < 0) {
                if (rb) {
                    leftUp.setPower(power + boostPower);
                    leftDown.setPower(0);
                    rightUp.setPower(0);
                    rightDown.setPower(power + boostPower);
                } else {
                    leftUp.setPower(power);
                    leftDown.setPower(0);
                    rightUp.setPower(0);
                    rightDown.setPower(power);
                }
            }

            telemetry.addData("Right Y", "%.2f", ry);
            telemetry.addData("Right X", "%.2f", rx);
            telemetry.addData("LeftUp", "%.2f", leftUp.getPower());
            telemetry.addData("LeftDown", "%.2f", leftDown.getPower());
            telemetry.addData("RightUp", "%.2f", rightUp.getPower());
            telemetry.addData("RightDown", "%.2f", rightDown.getPower());
            telemetry.addData("Boost", rb ? "ON" : "OFF");
            telemetry.update();
        }
    }
}