package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "test_encoder", group = "test")
public class TestEncoder extends LinearOpMode {
    private DcMotor leftUp, leftDown, rightUp, rightDown;

    @Override
    public void runOpMode() {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");

        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftUp.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        leftDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftDown.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        rightUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightUp.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        rightDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDown.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);


        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        leftUp.setTargetPosition(-1000);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.5);
        sleep(5000);
        leftUp.setTargetPosition(1000);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.5);
        sleep(3000);

        leftDown.setTargetPosition(-1000);
        leftDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDown.setPower(0.5);
        sleep(5000);
        leftDown.setTargetPosition(1000);
        leftDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDown.setPower(0.5);
        sleep(3000);

        rightUp.setTargetPosition(-1000);
        rightUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightUp.setPower(0.5);
        sleep(5000);
        rightUp.setTargetPosition(1000);
        rightUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightUp.setPower(0.5);
        sleep(3000);


        rightDown.setTargetPosition(-1000);
        rightDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDown.setPower(0.5);
        sleep(5000);
        rightDown.setTargetPosition(1000);
        rightDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDown.setPower(0.5);
        sleep(3000);

        while (opModeIsActive() && leftUp.isBusy()) {
            telemetry.addData("Status", "Running");
            telemetry.update();
        };

    }
}