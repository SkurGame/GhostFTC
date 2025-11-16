package org.firstinspires.ftc.teamcode.auto;

import android.net.http.UrlRequest;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@Autonomous(name = "BasicAuto")
public class Auto extends LinearOpMode {

    private DcMotor leftUp, leftDown, rightUp, rightDown;

    @Override
    public void runOpMode(){
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");
        rightUp.setDirection(DcMotor.Direction.REVERSE);
        rightDown.setDirection(DcMotor.Direction.REVERSE);

        leftUp.setPower(0);
        leftDown.setPower(0);
        rightUp.setPower(0);
        rightDown.setPower(0);

        waitForStart();
        telemetry.addData("Status", "Motor run");
        telemetry.update();

        leftUp.setPower(-0.5);
        leftDown.setPower(-0.5);
        rightUp.setPower(-0.5);
        rightDown.setPower(-0.5);

        sleep(500);

        leftUp.setPower(0);
        leftDown.setPower(0);
        rightUp.setPower(0);
        rightDown.setPower(0);

        telemetry.addData("Status", "Motor stop");
        telemetry.update();
    }
}
