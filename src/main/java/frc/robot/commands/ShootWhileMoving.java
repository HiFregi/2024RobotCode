// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants.ArmConstants;
import frc.robot.Constants.FieldConstants;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Swerve;
import frc.robot.util.AllianceUtil;

public class ShootWhileMoving extends Command {
  private final Arm arm;
  private final Intake intake;
  private final Swerve swerve;

  private Pose2d speakerPose;

  /** Creates a new ShootWhileMoving. */
  public ShootWhileMoving(Arm arm, Intake intake, Swerve swerve) {
    this.arm = arm;
    this.intake = intake;
    this.swerve = swerve;

    // Use addRequirements() here to declare subsystem dependencies.
    addRequirements(arm, intake);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    if (AllianceUtil.isRedAlliance()) {
      speakerPose = FieldConstants.speakerRedAlliance;
    } else {
      speakerPose = FieldConstants.speakerBlueAlliance;
    }
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    Translation2d robotPose = swerve.getPose().getTranslation();
    ChassisSpeeds fieldSpeeds = swerve.getFieldRelativeSpeeds();

    double distance = robotPose.minus(speakerPose.getTranslation()).getNorm();

    double shotTime = ArmConstants.autoShootTimeInterpolation.get(distance);

    double virtualGoalX = speakerPose.getX() + fieldSpeeds.vxMetersPerSecond * shotTime;
    double virtualGoalY = speakerPose.getY() + fieldSpeeds.vyMetersPerSecond * shotTime;

    Translation2d virtualGoalLocation = new Translation2d(virtualGoalX, virtualGoalY);

    for (int i = 0; i < 5; i++) {
      double newDistance = robotPose.minus(virtualGoalLocation).getNorm();
      double newShotTime = ArmConstants.autoShootTimeInterpolation.get(newDistance);

      if (Math.abs(shotTime - newShotTime) <= 0.05) {
        break;
      }

      virtualGoalX = speakerPose.getX() + fieldSpeeds.vxMetersPerSecond * newShotTime;
      virtualGoalY = speakerPose.getY() + fieldSpeeds.vyMetersPerSecond * newShotTime;

      virtualGoalLocation = new Translation2d(virtualGoalX, virtualGoalY);
      shotTime = newShotTime;
      distance = newDistance;
    }

    Rotation2d angle = Rotation2d.fromRadians(ArmConstants.autoShootInterpolation.get(distance));
    arm.setDesiredPosition(angle);

    Rotation2d angleError = arm.getPosition().minus(angle);

    if (Math.abs(MathUtil.inputModulus(angleError.getDegrees(), -180.0, 180.0)) < 0.25) {
      // Feed into shooter
    }
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    intake.stopIntakeMotor();
    // stop shooter
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
