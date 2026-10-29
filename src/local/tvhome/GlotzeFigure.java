package local.tvhome;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * The Glotze mascot: a green screen with two unequal eyes, drawn from the vector master in
 * assets/glotze. All coordinates are artwork units of the 673 x 504 view box. The paths are
 * rebuilt in pixels whenever the size changes, because Android 7 rasterises paths before
 * scaling them.
 */
final class GlotzeFigure {
    static final float ART_WIDTH = 673f;
    static final float ART_HEIGHT = 504f;

    private static final float ART_MARGIN = 32f;
    private static final int OPAQUE = 255;
    private static final int RGB_MASK = 0x00FFFFFF;
    private static final int ALPHA_SHIFT = 24;

    // Pivots of the artwork: the body turns and grows around the screen's centre, each eye
    // closes towards its own centre.
    private static final float BODY_PIVOT_X = 304.5f;
    private static final float BODY_PIVOT_Y = 220f;
    private static final float LEFT_EYE_PIVOT_X = 336.5f;
    private static final float LEFT_EYE_PIVOT_Y = 228f;
    private static final float RIGHT_EYE_PIVOT_X = 499.5f;
    private static final float RIGHT_EYE_PIVOT_Y = 233.5f;

    private static final String SCREEN_PATH =
            "M571.47 31.62" +
            "C600.53 57.97 605.9 102.09 608 139" +
            "C608.29 145.9 608.42 152.79 608.56 159.69" +
            "C608.58 160.71 608.6 161.74 608.62 162.79" +
            "C612.14 341.28 612.14 341.28 592 383" +
            "C591.62 383.79 591.24 384.57 590.86 385.37" +
            "C587.66 391.52 583.53 396.78 579 402" +
            "C578.34 402.77 577.69 403.53 577.01 404.32" +
            "C542.17 442.05 467.22 435.97 420.45 437.87" +
            "C315.66 441.98 209 443.6 108 431" +
            "C70 426.25 45 422 29 404" +
            "C8 380.375 2.61 348.91 1 319" +
            "C0.96 318.27 0.92 317.53 0.87 316.77" +
            "C0.07 301.86 -0.17 286.99 -0.17 272.06" +
            "C-0.17 269.71 -0.18 267.36 -0.18 265.01" +
            "C-0.19 260.12 -0.19 255.22 -0.19 250.33" +
            "C-0.19 245.39 -0.19 240.45 -0.21 235.51" +
            "C-0.56 105.79 -0.56 105.79 35 69" +
            "C54.93 50.07 81.56 43.82 108.31 43.75" +
            "C108.65 43.75 108.65 43.75 110.38 43.75" +
            "C125.82 43.73 141.21 44.65 156.62 45.5" +
            "C225.2 49.3 225.2 49.3 259 45" +
            "C260 44.88 261.01 44.76 262.04 44.63" +
            "C296.75 40.21 329.31 29.84 362.24 18.52" +
            "C380.63 12.2 398.78 6.34 418 3" +
            "C418.99 2.83 419.97 2.66 420.99 2.48" +
            "C469.95 -5.69 532.32 -2.11 571.47 31.62" +
            "Z";

    private static final String LEFT_EYE_PATH =
            "M367.38 114.12" +
            "C379.1 125.5 384.14 140.34 386.98 156.01" +
            "C387.14 156.97 387.32 157.93 387.5 158.91" +
            "C389.38 169.66 390.53 180.47 391.62 191.31" +
            "C391.84 193.39 392.06 195.47 392.26 197.56" +
            "C393.72 211.69 395.11 225.81 396 240" +
            "C396.06 240.88 396.12 241.75 396.18 242.66" +
            "C400.37 310.22 400.37 310.22 378.19 336.81" +
            "C368.32 347.27 354.25 354.56 339.74 355.34" +
            "C324.31 355.65 312.56 352.56 300.86 342.07" +
            "C280.52 321.39 281.17 284.66 279.12 257.84" +
            "C278.88 254.6 278.62 251.38 278.37 248.15" +
            "C277.88 242.09 277.44 236.05 277 230" +
            "C276.94 229.22 276.88 228.44 276.82 227.64" +
            "C275.99 216.11 275.78 204.57 275.75 193" +
            "C275.75 190 275.72 187 275.69 184.01" +
            "C275.56 159.9 279.52 136.34 296.8 118.22" +
            "C297.52 117.48 298.25 116.75 299 116" +
            "C299.55 115.44 300.08 114.88 300.64 114.28" +
            "C319.82 95.97 347.84 97.95 367.38 114.12" +
            "Z";

    private static final String RIGHT_EYE_PATH =
            "M523.51 144.1" +
            "C543.14 159.05 548.27 182.49 551.56 205.71" +
            "C553.77 222.29 555.26 238.82 555.31 255.56" +
            "C555.32 256.71 555.33 257.85 555.33 259.03" +
            "C555.15 280.82 550.05 301.38 534.71 317.49" +
            "C522.11 329.19 507.54 332.53 490.89 332.28" +
            "C480.95 331.89 472.25 328.88 465 322" +
            "C464.46 321.49 463.92 320.97 463.36 320.44" +
            "C453.63 310.17 451.04 295.08 449.56 281.5" +
            "C449.45 280.45 449.33 279.4 449.21 278.31" +
            "C447.98 266.68 447.02 255.02 446.09 243.36" +
            "C445.91 241.12 445.73 238.87 445.54 236.63" +
            "C444.55 224.55 443.84 212.52 443.81 200.4" +
            "C443.81 199.89 443.81 199.89 443.81 197.3" +
            "C443.91 178.66 449.51 162.19 462.19 148.31" +
            "C479.61 131.86 504.06 130.74 523.51 144.1" +
            "Z";

    /** How the figure is posed in one frame; the neutral pose is the artwork at rest. */
    static final class Pose {
        // Smaller changes are below what a screen shows: about a twentieth of a pixel at the usual size.
        private static final float POSITION_STEP = 0.05f;
        private static final float ANGLE_STEP = 0.01f;
        private static final float FACTOR_STEP = 0.0005f;

        float dy;
        float rotation;
        float scale = 1f;
        float opacity = 1f;
        float leftEyeScaleY = 1f;
        float rightEyeScaleY = 1f;
        float eyesDx;
        float eyesDy;

        void copyFrom(Pose other) {
            dy = other.dy;
            rotation = other.rotation;
            scale = other.scale;
            opacity = other.opacity;
            leftEyeScaleY = other.leftEyeScaleY;
            rightEyeScaleY = other.rightEyeScaleY;
            eyesDx = other.eyesDx;
            eyesDy = other.eyesDy;
        }

        boolean differsVisiblyFrom(Pose other) {
            return Math.abs(dy - other.dy) > POSITION_STEP
                    || Math.abs(eyesDx - other.eyesDx) > POSITION_STEP
                    || Math.abs(eyesDy - other.eyesDy) > POSITION_STEP
                    || Math.abs(rotation - other.rotation) > ANGLE_STEP
                    || Math.abs(scale - other.scale) > FACTOR_STEP
                    || Math.abs(opacity - other.opacity) > FACTOR_STEP
                    || Math.abs(leftEyeScaleY - other.leftEyeScaleY) > FACTOR_STEP
                    || Math.abs(rightEyeScaleY - other.rightEyeScaleY) > FACTOR_STEP;
        }
    }

    private final Part screen = new Part(SCREEN_PATH, BODY_PIVOT_X, BODY_PIVOT_Y);
    private final Part leftEye = new Part(LEFT_EYE_PATH, LEFT_EYE_PIVOT_X, LEFT_EYE_PIVOT_Y);
    private final Part rightEye = new Part(RIGHT_EYE_PATH, RIGHT_EYE_PIVOT_X, RIGHT_EYE_PIVOT_Y);
    private final Matrix fitting = new Matrix();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int bodyColor;
    private final int eyeColor;
    private float unit;

    GlotzeFigure(int bodyColor, int eyeColor) {
        this.bodyColor = bodyColor;
        this.eyeColor = eyeColor;
    }

    /** Centres the artwork in a view of this size, keeping its aspect ratio. */
    void fit(int width, int height) {
        unit = Math.min(width / ART_WIDTH, height / ART_HEIGHT);
        fitting.setTranslate(ART_MARGIN, ART_MARGIN);
        fitting.postScale(unit, unit);
        fitting.postTranslate((width - ART_WIDTH * unit) / 2f, (height - ART_HEIGHT * unit) / 2f);
        screen.fit(fitting);
        leftEye.fit(fitting);
        rightEye.fit(fitting);
    }

    void draw(Canvas canvas, Pose pose) {
        int alpha = Math.round(pose.opacity * OPAQUE);
        canvas.save();
        canvas.translate(0f, pose.dy * unit);
        canvas.rotate(pose.rotation, screen.pivotX, screen.pivotY);
        canvas.scale(pose.scale, pose.scale, screen.pivotX, screen.pivotY);
        drawPart(canvas, screen, bodyColor, alpha);
        canvas.translate(pose.eyesDx * unit, pose.eyesDy * unit);
        drawEye(canvas, leftEye, pose.leftEyeScaleY, alpha);
        drawEye(canvas, rightEye, pose.rightEyeScaleY, alpha);
        canvas.restore();
    }

    private void drawEye(Canvas canvas, Part eye, float scaleY, int alpha) {
        canvas.save();
        canvas.scale(1f, scaleY, eye.pivotX, eye.pivotY);
        drawPart(canvas, eye, eyeColor, alpha);
        canvas.restore();
    }

    private void drawPart(Canvas canvas, Part part, int color, int alpha) {
        paint.setColor((color & RGB_MASK) | (alpha << ALPHA_SHIFT));
        canvas.drawPath(part.fitted, paint);
    }

    /** A piece of the artwork in artwork units and in view pixels, with its pivot. */
    private static final class Part {
        final Path fitted = new Path();
        float pivotX;
        float pivotY;
        private final Path source;
        private final float sourcePivotX;
        private final float sourcePivotY;
        private final float[] point = new float[2];

        Part(String pathData, float sourcePivotX, float sourcePivotY) {
            this.source = parse(pathData);
            this.sourcePivotX = sourcePivotX;
            this.sourcePivotY = sourcePivotY;
        }

        void fit(Matrix matrix) {
            source.transform(matrix, fitted);
            point[0] = sourcePivotX;
            point[1] = sourcePivotY;
            matrix.mapPoints(point);
            pivotX = point[0];
            pivotY = point[1];
        }
    }

    /** Reads absolute M, C and Z, the only commands the Glotze mark uses. */
    private static Path parse(String data) {
        Path path = new Path();
        for (String segment : data.split("(?=[MCZ])")) {
            if (segment.isEmpty()) {
                continue; // Android 7 yields a leading empty piece for a split at position 0
            }
            float[] n = numbers(segment);
            switch (segment.charAt(0)) {
                case 'M':
                    path.moveTo(n[0], n[1]);
                    break;
                case 'C':
                    path.cubicTo(n[0], n[1], n[2], n[3], n[4], n[5]);
                    break;
                case 'Z':
                    path.close();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported path command in " + segment);
            }
        }
        return path;
    }

    private static float[] numbers(String segment) {
        String body = segment.substring(1).trim();
        if (body.isEmpty()) {
            return new float[0];
        }
        String[] words = body.split(" ");
        float[] values = new float[words.length];
        for (int i = 0; i < words.length; i++) {
            values[i] = Float.parseFloat(words[i]);
        }
        return values;
    }
}
