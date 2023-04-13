package io.github.lwlee2608.proto.gen;

import io.github.lwlee2608.proto.annotation.processor.ProtoFile;
import lombok.SneakyThrows;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;

public class ProtoGenImpl implements ProtoGen {
    private final CommandLineUtils.StringStreamConsumer error = new CommandLineUtils.StringStreamConsumer();
    private final CommandLineUtils.StringStreamConsumer output = new CommandLineUtils.StringStreamConsumer();

    @Override
    public void generate(Filer filer, List<ProtoFile> protoFiles) {
        protocGenerate(filer, protoFiles);
        //generateConverter(filer, protoFiles);
    }

    @SneakyThrows
    public void protocGenerate(Filer filer, List<ProtoFile> protoFiles) {
        System.out.println("Proto Gen Impl here!");

        // Register file to be generated by protoc to 'filer'.
        // If we skip this steps, generated file will not be compiled for some reason
        for (ProtoFile protoFile : protoFiles) {
            if (protoFile.getOuterClassName() == null) {
                continue;
            }
            String fullOuterClassName = protoFile.getPackageName() + "." + protoFile.getOuterClassName();
            JavaFileObject builderFile = filer.createSourceFile(fullOuterClassName);
            PrintWriter out = new PrintWriter(builderFile.openWriter());
            out.close();
        }

        // Get output directory
        FileObject resource = filer.getResource(StandardLocation.SOURCE_OUTPUT, "", "Dummy.java");
        String outputDirectory = Paths.get(resource.toUri()).toFile().getParent();

        // Generate using protoc
        String protoPath = protoFiles.get(0).getGeneratedFile().getParent();
        // Temp hardcoded Path
        String executable = System.getProperty("user.dir") + "/examples/target/protoc-binary/" + "protoc";
        String[] args = new String[]{
                "-I=.",
                "--java_out=" + outputDirectory,
                "--proto_path", protoPath};
        Commandline cl = new Commandline();
        cl.setExecutable(executable);
        cl.addArguments(args);
        protoFiles.forEach(protoFile -> cl.addArguments(new String[]{protoFile.getGeneratedFile().getAbsoluteFile().toString()}));

        int ret = CommandLineUtils.executeCommandLine(cl, null, output, error);
        if (ret != 0) {
            System.err.println("Error generating code using protoc: " + error.getOutput());
        }
    }
}
