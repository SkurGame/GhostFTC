package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Config;

@Autonomous(name = "test_motor", group = "Concept")
public class TestMotor extends LinearOpMode {
    private DcMotor leftUp;

    @Override
    public void runOpMode() {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftUp.setDirection(DcMotor.Direction.FORWARD);
        leftUp.setPower(0);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            leftUp.setPower(Config.motorPower.boost);

            telemetry.addData("Status", "Running");
            telemetry.addData("Motor Power", "%.2f", leftUp.getPower());
            telemetry.update();
        }

        leftUp.setPower(0);
        telemetry.addData("Status", "Stopped");
        telemetry.update();
    }
}