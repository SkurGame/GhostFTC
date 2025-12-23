package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "test_encoder", group = "test")
public class TestEncoder extends LinearOpMode {
    private DcMotor leftUp;

    @Override
    public void runOpMode() {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");

        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftUp.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        leftUp.setTargetPosition(-1000);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.5);
        sleep(10000);
        leftUp.setTargetPosition(1000);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.5);

        while (opModeIsActive() && leftUp.isBusy()) {
            telemetry.addData("Status", "Running");
            telemetry.addData("Position", "%d", leftUp.getCurrentPosition());
            telemetry.update();
        };

    }
}