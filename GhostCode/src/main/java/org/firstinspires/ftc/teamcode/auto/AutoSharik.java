package org.firstinspires.ftc.teamcode.auto;

import android.net.http.UrlRequest;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name = "AutoSharik")
public class AutoSharik extends LinearOpMode {

    private DcMotor leftUp, leftDown, rightUp, rightDown, strelylo;
    private Servo talkalo;

    @Override
    public void runOpMode(){
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");
        strelylo = hardwareMap.get(DcMotor.class, "strelylo");

        talkalo = hardwareMap.get(Servo.class, "talkalo");

        rightUp.setDirection(DcMotor.Direction.REVERSE);
        rightDown.setDirection(DcMotor.Direction.REVERSE);

        leftUp.setPower(0);
        leftDown.setPower(0);
        rightUp.setPower(0);
        rightDown.setPower(0);

        talkalo.setPosition(0.8);

        waitForStart();
        telemetry.addData("Status", "Motor run");
        telemetry.update();

        leftUp.setPower(0.5);
        leftDown.setPower(0.5);
        rightUp.setPower(0.5);
        rightDown.setPower(0.5);

        sleep(700);

        leftUp.setPower(0);
        leftDown.setPower(0);
        rightUp.setPower(0);
        rightDown.setPower(0);

        strelylo.setPower(0.6);
        sleep(2500);
        talkalo.setPosition(0);
        sleep(2500);
        talkalo.setPosition(1);
        strelylo.setPower(0);

        telemetry.update();
    }
}
