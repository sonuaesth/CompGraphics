# 3D Shape Stacking and Matching Puzzle

Java + JOGL project for the Computer Graphics course.

## What the program contains

- 3D stacking board with 4 pegs.
- Shape palette with 4 shapes: cube, sphere, cone, torus.
- Target configuration shown as wireframe shapes behind each peg.
- User stacks shown as solid colored shapes.
- Shape selection with keyboard or mouse.
- Shape placement, removal, rotation, and scaling.
- Ambient, diffuse, and specular lighting toggles.
- Solution checking and success message.

## Controls

- `1`, `2`, `3`, `4` - select shape.
- Mouse click on the left palette - select shape.
- `W`, `S` - switch active peg.
- `Space` - place selected shape on active peg.
- `Backspace` or `Delete` - remove top shape from active peg.
- `R` - rotate selected shape by 90 degrees.
- `+`, `-` - change selected shape scale.
- `L` - turn all lighting on/off.
- `A` - toggle ambient light.
- `D` - toggle diffuse light.
- `F` - toggle specular light.
- `C` - check solution.

## Running in VS Code on Windows

1. Install a JDK if `java` and `javac` are not available in the VS Code terminal.
   A safe choice is JDK 17.
2. Install the VS Code extension **Extension Pack for Java**.
3. Open the `2023230564_FINAL` folder in VS Code.
4. Open `src/Main.java`.
5. Use **Run and Debug** -> **Run 3D Stacking Puzzle**.

The JOGL `.jar` files are already referenced from the `../lib` folder in `.vscode/launch.json`.
