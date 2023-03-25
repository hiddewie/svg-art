# SVG

This project contains generated art using Kotlin source code, generating SVG files that can be converted to PDF.

## Generate

Compile the sources using an IDE like IntelliJ.

Run the main class `SvgKt`.

The output files can be found in the directory `out`.

Use the command 
```shell
./to-pdf
``` 
to convert the SVG files to PDF. It uses the library `rsvg-convert` to convert the files.