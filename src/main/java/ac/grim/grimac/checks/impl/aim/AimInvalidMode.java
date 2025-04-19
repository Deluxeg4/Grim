package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimInvalidMode")
public class AimInvalidMode extends Check implements RotationCheck {

    private double lastModeX = 0;
    private double lastModeY = 0;

    private int xRotationsSinceModeMismatch = 0;
    private int yRotationsSinceModeMismatch = 0;

    private int maxRotationsBuffer;

    public AimInvalidMode(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double currentModeX = rotationUpdate.getProcessor().modeX;
        double currentModeY = rotationUpdate.getProcessor().modeY;

        boolean modeMismatch = (currentModeX == lastModeX) != (currentModeY == lastModeY);
        boolean validLastModes = lastModeX != 0 && lastModeY != 0 && lastModeX < 1 && lastModeY < 1;

        if (modeMismatch && validLastModes) {
            boolean suspiciousX = isSuspiciousRotation(
                    rotationUpdate.getDeltaXRotABS(),
                    rotationUpdate.getProcessor().divisorX
            );
            boolean suspiciousY = isSuspiciousRotation(
                    rotationUpdate.getDeltaYRotABS(),
                    rotationUpdate.getProcessor().divisorY
            );

            if (suspiciousX) xRotationsSinceModeMismatch++;
            if (suspiciousY) yRotationsSinceModeMismatch++;

            if (xRotationsSinceModeMismatch > maxRotationsBuffer && yRotationsSinceModeMismatch > maxRotationsBuffer) {
                if (flagAndAlert("Detected invalid rotation mode mismatch (modeX=" + currentModeX +
                        ", lastModeX=" + lastModeX + ", modeY=" + currentModeY + ", lastModeY=" + lastModeY + ")")) {
                    // Optional: reset counts to avoid spamming
                    xRotationsSinceModeMismatch = 0;
                    yRotationsSinceModeMismatch = 0;
                }
            }

            // Do not reset counts here so it continues flagging
            return;
        }

        // Reset when modes sync again
        xRotationsSinceModeMismatch = 0;
        yRotationsSinceModeMismatch = 0;

        lastModeX = currentModeX;
        lastModeY = currentModeY;
    }

    private boolean isSuspiciousRotation(double delta, double divisor) {
        return delta > 0 && delta < 5 && divisor > GrimMath.MINIMUM_DIVISOR;
    }

    @Override
    public void onReload(ConfigManager config) {
        maxRotationsBuffer = config.getIntElse(getConfigName() + ".maxRots", 80);
    }
}
