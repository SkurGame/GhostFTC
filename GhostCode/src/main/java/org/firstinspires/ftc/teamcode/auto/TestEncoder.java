package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "TestEncoder")
public class TestEncoder extends LinearOpMode {
    private DcMotor leftUp;

    @Override
    public void runOpMode() throws InterruptedException {
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");

        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        waitForStart();

        leftUp.setTargetPosition(500);
        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftUp.setPower(0.5);

        while(opModeIsActive() && leftUp.isBusy()){
            telemetry.addData("Position", " ", leftUp.getCurrentPosition());
            telemetry.update();
        }

        leftUp.setPower(0);
    }
}
