package ogl2.practice;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

import java.util.ArrayList;
import java.util.List;

public class StackingPuzzle3D implements GLEventListener {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    private static final int FPS = 60;
    private static final int PEG_COUNT = 4;
    private static final int MAX_STACK_HEIGHT = 4;
    private static final float SLOT_HEIGHT = 0.55f;
    private static final float[] PEG_X = {-2.7f, -0.9f, 0.9f, 2.7f};
    private static final float[] SCALE_OPTIONS = {0.60f, 0.80f, 1.00f};

    private final GLU glu = new GLU();
    private final GLUT glut = new GLUT();
    private final List<PlacedShape>[] stacks = new ArrayList[PEG_COUNT];
    private final List<PlacedShape>[] targetStacks = new ArrayList[PEG_COUNT];

    private ShapeType selectedShape = ShapeType.CUBE;
    private int activePeg = 0;
    private int selectedRotation = 0;
    private float selectedScale = 1.0f;
    private boolean lightingEnabled = true;
    private boolean ambientEnabled = true;
    private boolean diffuseEnabled = true;
    private boolean specularEnabled = true;
    private String statusMessage = "Build the target stacks. Press C to check.";

    public static void main(String[] args) {
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDepthBits(24);

        GLWindow window = GLWindow.create(capabilities);
        StackingPuzzle3D puzzle = new StackingPuzzle3D();
        FPSAnimator animator = new FPSAnimator(window, FPS, true);

        window.addGLEventListener(puzzle);
        window.addKeyListener(puzzle.createKeyControls());
        window.addMouseListener(puzzle.createMouseControls(window));
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent event) {
                new Thread(() -> {
                    animator.stop();
                    System.exit(0);
                }).start();
            }
        });

        window.setTitle("3D Shape Stacking and Matching Puzzle");
        window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        window.setVisible(true);
        animator.start();
    }

    public StackingPuzzle3D() {
        for (int i = 0; i < PEG_COUNT; i++) {
            stacks[i] = new ArrayList<>();
            targetStacks[i] = new ArrayList<>();
        }
        createTargetConfiguration();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.08f, 0.09f, 0.11f, 1.0f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, new float[]{1f, 1f, 1f, 1f}, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 64f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        setupCamera(gl);
        setupLighting(gl);

        drawBoard(gl);
        drawTargetGhosts(gl);
        drawUserStacks(gl);
        drawPalette(gl);
        drawSelectedPreview(gl);
        drawOverlay(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        int safeHeight = Math.max(height, 1);
        gl.glViewport(0, 0, width, safeHeight);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, (double) width / safeHeight, 0.1, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private KeyAdapter createKeyControls() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_1:
                        selectShape(ShapeType.CUBE);
                        break;
                    case KeyEvent.VK_2:
                        selectShape(ShapeType.SPHERE);
                        break;
                    case KeyEvent.VK_3:
                        selectShape(ShapeType.CONE);
                        break;
                    case KeyEvent.VK_4:
                        selectShape(ShapeType.TORUS);
                        break;
                    case KeyEvent.VK_W:
                        activePeg = (activePeg + 1) % PEG_COUNT;
                        break;
                    case KeyEvent.VK_S:
                        activePeg = (activePeg + PEG_COUNT - 1) % PEG_COUNT;
                        break;
                    case KeyEvent.VK_SPACE:
                        placeShape();
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                    case KeyEvent.VK_DELETE:
                        removeShape();
                        break;
                    case KeyEvent.VK_R:
                        selectedRotation = (selectedRotation + 90) % 360;
                        break;
                    case KeyEvent.VK_EQUALS:
                    case KeyEvent.VK_PLUS:
                        changeScale(1);
                        break;
                    case KeyEvent.VK_MINUS:
                        changeScale(-1);
                        break;
                    case KeyEvent.VK_L:
                        lightingEnabled = !lightingEnabled;
                        break;
                    case KeyEvent.VK_A:
                        ambientEnabled = !ambientEnabled;
                        break;
                    case KeyEvent.VK_D:
                        diffuseEnabled = !diffuseEnabled;
                        break;
                    case KeyEvent.VK_F:
                        specularEnabled = !specularEnabled;
                        break;
                    case KeyEvent.VK_C:
                        checkSolution();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private MouseAdapter createMouseControls(GLWindow window) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getX() > 170) {
                    return;
                }
                int sectionHeight = Math.max(1, window.getHeight() / ShapeType.values().length);
                int index = Math.min(ShapeType.values().length - 1, event.getY() / sectionHeight);
                selectShape(ShapeType.values()[index]);
            }
        };
    }

    private void createTargetConfiguration() {
        addTarget(0, ShapeType.CUBE, 0, 1.0f);
        addTarget(0, ShapeType.TORUS, 90, 0.80f);

        addTarget(1, ShapeType.SPHERE, 0, 0.80f);
        addTarget(1, ShapeType.CONE, 0, 1.0f);
        addTarget(1, ShapeType.CUBE, 0, 0.60f);

        addTarget(2, ShapeType.TORUS, 0, 1.0f);
        addTarget(2, ShapeType.TORUS, 90, 0.80f);

        addTarget(3, ShapeType.CONE, 0, 0.80f);
        addTarget(3, ShapeType.SPHERE, 0, 0.60f);
        addTarget(3, ShapeType.CUBE, 0, 0.60f);
    }

    private void addTarget(int peg, ShapeType shape, int rotation, float scale) {
        targetStacks[peg].add(new PlacedShape(shape, rotation, scale));
    }

    private void setupCamera(GL2 gl) {
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(0.0, 5.4, 8.8, 0.0, 0.6, 0.0, 0.0, 1.0, 0.0);
    }

    private void setupLighting(GL2 gl) {
        if (!lightingEnabled) {
            gl.glDisable(GL2.GL_LIGHTING);
            return;
        }

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{-3f, 6f, 5f, 1f}, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientEnabled ? new float[]{0.25f, 0.25f, 0.28f, 1f} : new float[]{0f, 0f, 0f, 1f}, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseEnabled ? new float[]{0.8f, 0.8f, 0.75f, 1f} : new float[]{0f, 0f, 0f, 1f}, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularEnabled ? new float[]{0.95f, 0.95f, 1f, 1f} : new float[]{0f, 0f, 0f, 1f}, 0);
    }

    private void drawBoard(GL2 gl) {
        gl.glPushMatrix();
        gl.glColor3f(0.42f, 0.33f, 0.23f);
        gl.glTranslatef(0f, -0.08f, 0f);
        gl.glScalef(7.4f, 0.16f, 2.7f);
        glut.glutSolidCube(1f);
        gl.glPopMatrix();

        for (int i = 0; i < PEG_COUNT; i++) {
            gl.glPushMatrix();
            gl.glTranslatef(PEG_X[i], 1.05f, 0f);
            gl.glColor3f(i == activePeg ? 0.95f : 0.64f, i == activePeg ? 0.82f : 0.66f, i == activePeg ? 0.25f : 0.72f);
            gl.glRotatef(-90f, 1f, 0f, 0f);
            glut.glutSolidCylinder(0.08, 2.2, 24, 8);
            gl.glPopMatrix();
        }
    }

    private void drawTargetGhosts(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.88f, 0.88f, 0.88f);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);

        for (int peg = 0; peg < PEG_COUNT; peg++) {
            for (int level = 0; level < targetStacks[peg].size(); level++) {
                PlacedShape target = targetStacks[peg].get(level);
                drawShapeAt(gl, target, PEG_X[peg], 0.3f + level * SLOT_HEIGHT, -0.55f, false);
            }
        }

        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        if (lightingEnabled) {
            gl.glEnable(GL2.GL_LIGHTING);
        }
    }

    private void drawUserStacks(GL2 gl) {
        for (int peg = 0; peg < PEG_COUNT; peg++) {
            for (int level = 0; level < stacks[peg].size(); level++) {
                PlacedShape placed = stacks[peg].get(level);
                drawShapeAt(gl, placed, PEG_X[peg], 0.3f + level * SLOT_HEIGHT, 0.15f, true);
            }
        }
    }

    private void drawPalette(GL2 gl) {
        float y = 2.6f;
        for (ShapeType shape : ShapeType.values()) {
            PlacedShape sample = new PlacedShape(shape, 0, 0.8f);
            drawShapeAt(gl, sample, -4.7f, y, 0f, true);
            if (shape == selectedShape) {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glColor3f(1f, 1f, 1f);
                gl.glPushMatrix();
                gl.glTranslatef(-4.7f, y, 0f);
                glut.glutWireCube(1.05f);
                gl.glPopMatrix();
                if (lightingEnabled) {
                    gl.glEnable(GL2.GL_LIGHTING);
                }
            }
            y -= 1.0f;
        }
    }

    private void drawSelectedPreview(GL2 gl) {
        PlacedShape preview = new PlacedShape(selectedShape, selectedRotation, selectedScale);
        drawShapeAt(gl, preview, PEG_X[activePeg], 2.8f, 0.2f, true);
    }

    private void drawShapeAt(GL2 gl, PlacedShape shape, float x, float y, float z, boolean useShapeColor) {
        gl.glPushMatrix();
        gl.glTranslatef(x, y, z);
        gl.glRotatef(shape.rotationDegrees(), 0f, 1f, 0f);
        gl.glScalef(shape.scale(), shape.scale(), shape.scale());
        if (useShapeColor) {
            setShapeColor(gl, shape.type());
        }
        drawShapeGeometry(gl, shape.type());
        gl.glPopMatrix();
    }

    private void drawShapeGeometry(GL2 gl, ShapeType shape) {
        switch (shape) {
            case CUBE:
                glut.glutSolidCube(0.65f);
                break;
            case SPHERE:
                glut.glutSolidSphere(0.38, 32, 16);
                break;
            case CONE:
                gl.glPushMatrix();
                gl.glTranslatef(0f, -0.32f, 0f);
                gl.glRotatef(-90f, 1f, 0f, 0f);
                glut.glutSolidCone(0.42, 0.7, 32, 16);
                gl.glPopMatrix();
                break;
            case TORUS:
                glut.glutSolidTorus(0.13, 0.33, 24, 32);
                break;
            default:
                break;
        }
    }

    private void drawOverlay(GL2 gl, int width, int height) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, 0, height);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_DEPTH_TEST);

        gl.glColor3f(0.95f, 0.95f, 0.95f);
        drawText(gl, 18, height - 28, "1-4 select shape | Mouse left palette | W/S peg | Space place | Backspace remove");
        drawText(gl, 18, height - 50, "R rotate torus | +/- scale preset | L light | A ambient | D diffuse | F specular | C check");
        drawText(gl, 18, 24, "Selected: " + selectedShape.label() + " | Peg: " + (activePeg + 1) + " | Rot: " + selectedRotation + " | Scale: " + String.format("%.2f", selectedScale));
        drawText(gl, 18, 46, statusMessage);

        gl.glEnable(GL.GL_DEPTH_TEST);
        if (lightingEnabled) {
            gl.glEnable(GL2.GL_LIGHTING);
        }
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private void drawText(GL2 gl, int x, int y, String text) {
        gl.glRasterPos2i(x, y);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, text);
    }

    private void setShapeColor(GL2 gl, ShapeType shape) {
        float[] color = shape.color();
        gl.glColor3f(color[0], color[1], color[2]);
    }

    private void selectShape(ShapeType shape) {
        selectedShape = shape;
        statusMessage = "Selected " + shape.label() + ".";
    }

    private void placeShape() {
        if (stacks[activePeg].size() >= MAX_STACK_HEIGHT) {
            statusMessage = "This peg is full.";
            return;
        }
        stacks[activePeg].add(new PlacedShape(selectedShape, selectedRotation, selectedScale));
        statusMessage = "Placed " + selectedShape.label() + " on peg " + (activePeg + 1) + ".";
    }

    private void removeShape() {
        if (stacks[activePeg].isEmpty()) {
            statusMessage = "Peg " + (activePeg + 1) + " is already empty.";
            return;
        }
        stacks[activePeg].remove(stacks[activePeg].size() - 1);
        statusMessage = "Removed top shape from peg " + (activePeg + 1) + ".";
    }

    private void changeScale(int direction) {
        int index = closestScaleIndex();
        index += direction;
        if (index < 0) {
            index = SCALE_OPTIONS.length - 1;
        } else if (index >= SCALE_OPTIONS.length) {
            index = 0;
        }
        selectedScale = SCALE_OPTIONS[index];
        statusMessage = "Scale changed to " + String.format("%.2f", selectedScale) + ".";
    }

    private int closestScaleIndex() {
        int closest = 0;
        float bestDistance = Math.abs(selectedScale - SCALE_OPTIONS[0]);
        for (int i = 1; i < SCALE_OPTIONS.length; i++) {
            float distance = Math.abs(selectedScale - SCALE_OPTIONS[i]);
            if (distance < bestDistance) {
                closest = i;
                bestDistance = distance;
            }
        }
        return closest;
    }

    private void checkSolution() {
        for (int peg = 0; peg < PEG_COUNT; peg++) {
            if (stacks[peg].size() != targetStacks[peg].size()) {
                statusMessage = "Peg " + (peg + 1) + " has wrong number of shapes.";
                return;
            }
            for (int level = 0; level < targetStacks[peg].size(); level++) {
                PlacedShape actual = stacks[peg].get(level);
                PlacedShape target = targetStacks[peg].get(level);
                if (!actual.matches(target)) {
                    statusMessage = "Peg " + (peg + 1) + ", level " + (level + 1)
                            + ": expected " + target.describe()
                            + ", got " + actual.describe() + ".";
                    return;
                }
            }
        }
        statusMessage = "Success! All stacks match the target configuration.";
    }

    private static class PlacedShape {
        private final ShapeType type;
        private final int rotationDegrees;
        private final float scale;

        PlacedShape(ShapeType type, int rotationDegrees, float scale) {
            this.type = type;
            this.rotationDegrees = rotationDegrees;
            this.scale = scale;
        }

        ShapeType type() {
            return type;
        }

        int rotationDegrees() {
            return rotationDegrees;
        }

        float scale() {
            return scale;
        }

        boolean matches(PlacedShape other) {
            return type == other.type
                    && normalizedRotation() == other.normalizedRotation()
                    && Math.abs(scale - other.scale) < 0.01f;
        }

        String describe() {
            if (!type.usesRotation()) {
                return type.label() + " scale " + String.format("%.2f", scale);
            }
            return type.label() + " rot " + normalizedRotation() + " scale " + String.format("%.2f", scale);
        }

        private int normalizedRotation() {
            if (!type.usesRotation()) {
                return 0;
            }
            return rotationDegrees;
        }
    }

    private enum ShapeType {
        CUBE("Cube", new float[]{0.95f, 0.24f, 0.25f}),
        SPHERE("Sphere", new float[]{0.2f, 0.65f, 0.95f}),
        CONE("Cone", new float[]{0.28f, 0.82f, 0.38f}),
        TORUS("Torus", new float[]{0.95f, 0.72f, 0.18f});

        private final String label;
        private final float[] color;

        ShapeType(String label, float[] color) {
            this.label = label;
            this.color = color;
        }

        String label() {
            return label;
        }

        float[] color() {
            return color;
        }

        boolean usesRotation() {
            return this == TORUS;
        }
    }
}
