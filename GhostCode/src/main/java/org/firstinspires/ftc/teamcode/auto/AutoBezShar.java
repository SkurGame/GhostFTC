package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.util.EncoderConverter;

@Autonomous(name = "AutoBezShar", group = "auto")
public class AutoBezShar extends LinearOpMode {

    private DcMotor leftUp, leftDown, rightUp, rightDown, strelylo, sasat;
    private Servo talkalo;
    private EncoderConverter encoderConverter;
    private ElapsedTime runtime = new ElapsedTime();

    private void driveDistance(double distanceCM, double power, double timeoutS) {
        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        int targetTicks = encoderConverter.cmToTicks(distanceCM);

        leftUp.setTargetPosition(targetTicks);
        rightUp.setTargetPosition(targetTicks);
        leftDown.setTargetPosition(targetTicks);
        rightDown.setTargetPosition(targetTicks);

        leftUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightUp.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDown.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftUp.setPower(Math.abs(power));
        rightUp.setPower(Math.abs(power));
        leftDown.setPower(Math.abs(power));
        rightDown.setPower(Math.abs(power));

        runtime.reset();

        while (opModeIsActive() &&
                leftUp.isBusy() && rightUp.isBusy() &&
                leftDown.isBusy() && rightDown.isBusy() &&
                runtime.seconds() < timeoutS) {
        }

        leftUp.setPower(0);
        rightUp.setPower(0);
        leftDown.setPower(0);
        rightDown.setPower(0);

        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        sleep(250);
    }

    @Override
    public void runOpMode(){
        leftUp = hardwareMap.get(DcMotor.class, "leftUp");
        leftDown = hardwareMap.get(DcMotor.class, "leftDown");
        rightUp = hardwareMap.get(DcMotor.class, "rightUp");
        rightDown = hardwareMap.get(DcMotor.class, "rightDown");
        strelylo = hardwareMap.get(DcMotor.class, "strelylo");
        sasat = hardwareMap.get(DcMotor.class, "sasat");
        talkalo = hardwareMap.get(Servo.class, "talkalo");

        rightUp.setDirection(DcMotor.Direction.REVERSE);
        rightDown.setDirection(DcMotor.Direction.REVERSE);

        encoderConverter = new EncoderConverter(28, 19.2, 9.6);

        leftUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightUp.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightDown.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightDown.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightUp.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        talkalo.setPosition(0.8);

        waitForStart();

        driveDistance(100.0, 0.5, 10.0);

        while (opModeIsActive()){
            telemetry.addData("Position", "%.2f", talkalo.getPosition());
            telemetry.update();
        }
    }
}