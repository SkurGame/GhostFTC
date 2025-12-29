package org.firstinspires.ftc.teamcode.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.Config;

@TeleOp(name = "TeleOp", group = "teleop")
public class BasicTeleOp extends LinearOpMode {
    private DcMotor leftUp, leftDown, rightUp, rightDown, pytka, strelylo, sasat;
    private Servo talkalo;

    // Переменные для управления сосало
    private boolean sasatActive = false;
    private double sasatPower = 0;
    private long sasatEndTime = 0;
    private boolean sasatWithTalkalo = false;
    private double talkaloTargetPosition = 1;

    // Переменные для управления стреляло
    private boolean strelyloActive = false;
    private int strelyloStage = 0;
    private long strelyloStageTime = 0;

    // Переменные для управления пяткой
    private boolean pytkaActive = false;
    private double pytkaPower = 0;
    private long pytkaEndTime = 0;

    public void runOpMode() {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");
        pytka = hardwareMap.get(DcMotor.class, "pytka");
        strelylo = hardwareMap.get(DcMotor.class, "strelylo");
        sasat = hardwareMap.get(DcMotor.class, "sasat");

        talkalo = hardwareMap.get(Servo.class, "talkalo");

        rightUp.setDirection(DcMotor.Direction.REVERSE);
        rightDown.setDirection(DcMotor.Direction.REVERSE);

        leftUp.setPower(0);
        leftDown.setPower(0);
        rightUp.setPower(0);
        rightDown.setPower(0);
        sasat.setPower(0);
        strelylo.setPower(0);
        pytka.setPower(0);

        talkalo.setPosition(1);

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

            long currentTime = System.currentTimeMillis();

            if (gamepad2.a && !sasatActive && !strelyloActive) {
                sasatPower = 1;
                sasatActive = true;
                sasatEndTime = currentTime + 5000;
                sasatWithTalkalo = false;
            }

            if (gamepad2.b && !sasatActive && !strelyloActive) {
                sasatPower = -1;
                sasatActive = true;
                sasatEndTime = currentTime + 5000;
                sasatWithTalkalo = true;
                talkaloTargetPosition = 0.1;
            }

            if (sasatActive) {
                sasat.setPower(sasatPower);
                if (sasatWithTalkalo) {
                    talkalo.setPosition(talkaloTargetPosition);
                }

                if (currentTime >= sasatEndTime) {
                    sasatActive = false;
                    sasat.setPower(0);
                    if (sasatWithTalkalo) {
                        talkaloTargetPosition = 1;
                    }
                }
            } else {
                sasat.setPower(0);
            }

            // СТРЕЛЯЛО
            if(gamepad2.y){
                strelylo.setPower(0.7);
                sasat.setPower(1);
                sleep(2500);
                talkalo.setPosition(0);
                sleep(500);
                talkalo.setPosition(1);
                strelylo.setPower(0.1);
            }

            if (gamepad1.dpad_up && !pytkaActive) {
                pytkaPower = -0.7;
                pytkaActive = true;
                pytkaEndTime = currentTime + 500;
            }

            if (gamepad1.dpad_down && !pytkaActive) {
                pytkaPower = 0.7;
                pytkaActive = true;
                pytkaEndTime = currentTime + 200;
            }

            if (pytkaActive) {
                pytka.setPower(pytkaPower);
                if (currentTime >= pytkaEndTime) {
                    pytkaActive = false;
                    pytka.setPower(0);
                }
            }

            double leftUpPower = 0;
            double leftDownPower = 0;
            double rightUpPower = 0;
            double rightDownPower = 0;

            // ПОВОРОТ
            if (gamepad1.left_trigger > 0.1) {
                if (rb) {
                    leftUpPower = power + boostPower;
                    leftDownPower = power + boostPower;
                    rightUpPower = -power - boostPower;
                    rightDownPower = -power - boostPower;
                } else {
                    leftUpPower = power;
                    leftDownPower = power;
                    rightUpPower = -power;
                    rightDownPower = -power;
                }
            }
            else if (gamepad1.right_trigger > 0.1) {
                if (rb) {
                    leftUpPower = -power - boostPower;
                    leftDownPower = -power - boostPower;
                    rightUpPower = power + boostPower;
                    rightDownPower = power + boostPower;
                } else {
                    leftUpPower = -power;
                    leftDownPower = -power;
                    rightUpPower = power;
                    rightDownPower = power;
                }
            }
            // ДВИЖЕНИЕ ВПЕРЕД
            else if (ry > 0 && Math.abs(rx) < 0.2) {
                if (rb) {
                    leftUpPower = power + boostPower;
                    leftDownPower = power + boostPower;
                    rightUpPower = power + boostPower;
                    rightDownPower = power + boostPower;
                } else {
                    leftUpPower = power;
                    leftDownPower = power;
                    rightUpPower = power;
                    rightDownPower = power;
                }
            }
            // ДВИЖЕНИЕ НАЗАД
            else if (ry < 0 && Math.abs(rx) < 0.2) {
                if (rb) {
                    leftUpPower = -power - boostPower;
                    leftDownPower = -power - boostPower;
                    rightUpPower = -power - boostPower;
                    rightDownPower = -power - boostPower;
                } else {
                    leftUpPower = -power;
                    leftDownPower = -power;
                    rightUpPower = -power;
                    rightDownPower = -power;
                }
            }
            // ДВИЖЕНИЕ ВПРАВО
            else if (rx > 0 && Math.abs(ry) < 0.2) {
                if (rb) {
                    leftUpPower = -power - boostPower;
                    leftDownPower = power + boostPower;
                    rightUpPower = power + boostPower;
                    rightDownPower = -power - boostPower;
                } else {
                    leftUpPower = -power;
                    leftDownPower = power;
                    rightUpPower = power;
                    rightDownPower = -power;
                }
            }
            // ДВИЖЕНИЕ ВЛЕВО
            else if (rx < 0 && Math.abs(ry) < 0.2) {
                if (rb) {
                    leftUpPower = power + boostPower;
                    leftDownPower = -power - boostPower;
                    rightUpPower = -power - boostPower;
                    rightDownPower = power + boostPower;
                } else {
                    leftUpPower = power;
                    leftDownPower = -power;
                    rightUpPower = -power;
                    rightDownPower = power;
                }
            }
            // ДИАГОНАЛЬ ВПЕРЕД-ВПРАВО
            else if (ry < 0 && rx > 0) {
                if (rb) {
                    leftUpPower = -power - boostPower;
                    leftDownPower = 0;
                    rightUpPower = 0;
                    rightDownPower = -power - boostPower;
                } else {
                    leftUpPower = -power;
                    leftDownPower = 0;
                    rightUpPower = 0;
                    rightDownPower = -power;
                }
            }
            // ДИАГОНАЛЬ ВПЕРЕД-ВЛЕВО
            else if (ry < 0 && rx < 0) {
                if (rb) {
                    leftUpPower = 0;
                    leftDownPower = -power - boostPower;
                    rightUpPower = -power - boostPower;
                    rightDownPower = 0;
                } else {
                    leftUpPower = 0;
                    leftDownPower = -power;
                    rightUpPower = -power;
                    rightDownPower = 0;
                }
            }
            // ДИАГОНАЛЬ НАЗАД-ВПРАВО
            else if (ry > 0 && rx > 0) {
                if (rb) {
                    leftUpPower = 0;
                    leftDownPower = power + boostPower;
                    rightUpPower = power + boostPower;
                    rightDownPower = 0;
                } else {
                    leftUpPower = 0;
                    leftDownPower = power;
                    rightUpPower = power;
                    rightDownPower = 0;
                }
            }
            // ДИАГОНАЛЬ НАЗАД-ВЛЕВО
            else if (ry > 0 && rx < 0) {
                if (rb) {
                    leftUpPower = power + boostPower;
                    leftDownPower = 0;
                    rightUpPower = 0;
                    rightDownPower = power + boostPower;
                } else {
                    leftUpPower = power;
                    leftDownPower = 0;
                    rightUpPower = 0;
                    rightDownPower = power;
                }
            }

            // Применяем мощности к моторам движения
            leftUp.setPower(leftUpPower);
            leftDown.setPower(leftDownPower);
            rightUp.setPower(rightUpPower);
            rightDown.setPower(rightDownPower);

            // Управление сервой (если не управляется другими процессами)
            if (!strelyloActive && !sasatActive && !sasatWithTalkalo) {
                talkalo.setPosition(talkaloTargetPosition);
            }

            // Телеметрия
            telemetry.addData("Status", "Running");
            telemetry.addData("Sasat Active", sasatActive ? "YES" : "NO");
            telemetry.addData("Strelylo Active", strelyloActive ? "YES" : "NO");
            telemetry.addData("Pytka Active", pytkaActive ? "YES" : "NO");
            telemetry.addData("Sasat Power", "%.2f", sasat.getPower());
            telemetry.addData("Strelylo Power", "%.2f", strelylo.getPower());
            telemetry.addData("Pytka Power", "%.2f", pytka.getPower());
            telemetry.addData("Right Y", "%.2f", ry);
            telemetry.addData("Right X", "%.2f", rx);
            telemetry.addData("LeftUp", "%.2f", leftUpPower);
            telemetry.addData("LeftDown", "%.2f", leftDownPower);
            telemetry.addData("RightUp", "%.2f", rightUpPower);
            telemetry.addData("RightDown", "%.2f", rightDownPower);
            telemetry.addData("ServoPosition", "%.2f", talkalo.getPosition());
            telemetry.addData("Boost", rb ? "ON" : "OFF");
            telemetry.update();
        }
    }
}