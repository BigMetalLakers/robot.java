// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.photonvision.PhotonCamera; //https://maven.photonvision.org/repository/internal/org/photonvision/PhotonLib-json/1.0/PhotonLib-json-1.0.json

import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.CANSparkMax;//https://software-metadata.revrobotics.com/REVLib-2024.json

import edu.wpi.first.wpilibj.ADIS16470_IMU;
import edu.wpi.first.wpilibj.CounterBase;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.SPI; // required for ADIS IMUs
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.motorcontrol.Spark;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
  private final Spark m_rightDrive = new Spark(1);
  private final Spark m_leftDrive = new Spark(0);
  private final Spark intakeRollerMotor = new Spark(2);

  private CANSparkMax Shooter_R = new CANSparkMax(24, MotorType.kBrushless);
  private CANSparkMax Shooter_L = new CANSparkMax(23, MotorType.kBrushless);
  private CANSparkMax Loader_R = new CANSparkMax(22, MotorType.kBrushless);
  private CANSparkMax Loader_L = new CANSparkMax(21, MotorType.kBrushless);
  private CANSparkMax intakeNeo = new CANSparkMax(3, MotorType.kBrushless);
  private final DifferentialDrive m_robotDrive = new DifferentialDrive(m_leftDrive, m_rightDrive);
  private final XboxController m_stick = new XboxController(0);
  private final Timer m_timer = new Timer();
  private final Encoder right_encoder = new Encoder(6, 7, true, CounterBase.EncodingType.k4X);
  private final Encoder left_encoder = new Encoder(8, 9, false, CounterBase.EncodingType.k4X);
  public static final ADIS16470_IMU imu = new ADIS16470_IMU(ADIS16470_IMU.IMUAxis.kY, ADIS16470_IMU.IMUAxis.kX, ADIS16470_IMU.IMUAxis.kZ, SPI.Port.kOnboardCS0, ADIS16470_IMU.CalibrationTime._1s);
  PhotonCamera noteCamera = new PhotonCamera("AVerMedia_PW315");
  //PhotonCamera frontCamera = new PhotonCamera("Microsoft_LifeCam_HD-3000");
  public double SpeedForward, rotationClockwise, cYaw;
  public static boolean pHasTarget;
  public static double intakeSpeed = 0;
  public static double MaxIntakeSpeed = 1;
  public static double loaderSpeed = 0;
  public static double Sharpness = 0.5;
  public static double MaxTurnEffort = 0.35;
  public static double noteYaw = 0.0;
  public static double tYaw = 0.0;
  

  @Override
  public void robotInit() {
    imu.calibrate();
    m_rightDrive.setInverted(true);
    right_encoder.setSamplesToAverage(5);
    left_encoder.setSamplesToAverage(5);
    right_encoder.setDistancePerPulse(10.0 * 0.3048 / 12928); // pulses averaged from l & r = 12928 of 10' (5 x 2' tiles)
    left_encoder.setDistancePerPulse(10.0 * 0.3048 / 12928); // 0.3048 converts feet into metres
    right_encoder.setMinRate(0.01);
    left_encoder.setMinRate(0.01);
    right_encoder.reset();
    left_encoder.reset();
    Loader_L.setIdleMode(IdleMode.kBrake);
    Loader_R.setIdleMode(IdleMode.kBrake);
    SmartDashboard.putNumber("MaxTurnEffort", MaxTurnEffort);
    SmartDashboard.putNumber("Sharpness", Sharpness);
    m_timer.reset();
    m_timer.start();
  }

  @Override
  public void robotPeriodic() {
    cYaw = -imu.getAngle(ADIS16470_IMU.IMUAxis.kY) % 360;
    var result = noteCamera.getLatestResult();
    pHasTarget = result.hasTargets();
    SmartDashboard.putBoolean("Has Target",pHasTarget);
    if (pHasTarget) {
    noteYaw = result.getBestTarget().getYaw();
    }
    // //consider an additional conditional to make sure the target for tYaw is
    // tYaw = cYaw - pTargetYaw;
    // SmartDashboard.putNumber("TargetID", result.getBestTarget().getFiducialId());
    // SmartDashboard.putNumber("Target Yaw", pTargetYaw);
    // }

    SmartDashboard.putNumber("Angle", cYaw);
    SmartDashboard.putNumber("R_Encoder Distance", right_encoder.getDistance());
    SmartDashboard.putNumber("L_Encoder Distance", left_encoder.getDistance());
    SmartDashboard.putNumber("R_Encoder Speed", right_encoder.getRate());
    SmartDashboard.putNumber("L_Encoder Speed", left_encoder.getRate());
    SmartDashboard.putNumber("noteYaw", noteYaw);
    SmartDashboard.putNumber("R_Encoder", right_encoder.get());
    SmartDashboard.putNumber("L_Encoder", left_encoder.get());
    SmartDashboard.putNumber("Rotation Clockwise", rotationClockwise);
    SmartDashboard.putNumber("SpeedForward", SpeedForward);
    Sharpness = SmartDashboard.getNumber("Sharpness",Sharpness);
    MaxTurnEffort = SmartDashboard.getNumber("MaxTurnEffort", MaxTurnEffort);
  }

  @Override
  public void autonomousInit() {
    m_timer.reset();
    right_encoder.reset();
    left_encoder.reset();
    m_timer.start();
    imu.reset();
  }

  @Override
  public void autonomousPeriodic() {
    intakeNeo.set(0.25);
    intakeRollerMotor.set(-1);
    if (m_timer.get() >= 0 && m_timer.get() <= 2) {
      Shooter_L.set(1);
      Shooter_R.set(-1);
    } else if (m_timer.get() >= 2 && m_timer.get() <= 4) {
      Loader_L.set(0.8);
      Loader_R.set(-0.8);
    } else if (m_timer.get() >= 4 && m_timer.get() <= 7) {
      m_robotDrive.arcadeDrive(-0.6, 0);
      Loader_L.set(-0.05);
      Loader_R.set(-0.05);
      Shooter_L.set(-0.1);
      Shooter_R.set(-0.1);
    } else if (m_timer.get() >= 7.5 && m_timer.get() <= 10) {
      m_robotDrive.arcadeDrive(0.71, 0);
    } else if (m_timer.get() >= 10 && m_timer.get() <= 12) {
      Shooter_L.set(1);
      Shooter_R.set(-1);
    } else if (m_timer.get() >= 12 && m_timer.get() <= 14) {
      Loader_L.set(0.6);
      Loader_R.set(-0.6);
    } else {
      Shooter_L.set(0);
      Shooter_R.set(0);
      Loader_L.set(0);
      Loader_R.set(0);
      intakeNeo.set(0);
      intakeRollerMotor.set(0);
    }
  }

  @Override
  public void teleopPeriodic() {
    if (m_stick.getLeftBumper()) {
      right_encoder.reset();
      left_encoder.reset();
    }

    if (m_stick.getRightBumper()) {
      imu.reset();
    }

    // forward & backwards movement
    SpeedForward = (m_stick.getRightTriggerAxis() - m_stick.getLeftTriggerAxis());

    // rotation
    rotationClockwise = m_stick.getLeftX() * -0.6;

    // intake forward & backwards
    intakeSpeed = m_stick.getAButton() ? MaxIntakeSpeed : 0;
    intakeSpeed = m_stick.getYButton() ? -MaxIntakeSpeed : intakeSpeed;

    // set methods
    Loader_L.set(loaderSpeed);
    Loader_R.set(-loaderSpeed);
    Shooter_L.set(0);
    Shooter_R.set(0);

    // shoot
    if (m_stick.getBButton()) {
      if (m_timer.get() >= 0 && m_timer.get() <= 2.6) {
        Shooter_L.set(1);
        Shooter_R.set(-1);
      }
      if (m_timer.get() >= 2 && m_timer.get() <= 4) {
        Loader_L.set(0.3);
        Loader_R.set(-0.3);
      }
    } else {
      m_timer.reset();
    }

    intakeNeo.set(intakeSpeed * 0.25);
    intakeRollerMotor.set(-intakeSpeed);
    
    // contol sensors
    // reset encoders with POV up arrow
    //if (m_stick.getPOV() == 0 ){ 
    //right_encoder.reset();
    //left_encoder.reset();
    //}

    // steer to follow a note
    if (m_stick.getPOV()==0 && pHasTarget) {
      rotationClockwise =(1-Math.exp(-Sharpness*Math.pow(noteYaw,2)))*(-1)*Math.signum(noteYaw)*MaxTurnEffort;
    }
  


    // Drive
    m_robotDrive.arcadeDrive(SpeedForward, rotationClockwise, false);
  }
}
