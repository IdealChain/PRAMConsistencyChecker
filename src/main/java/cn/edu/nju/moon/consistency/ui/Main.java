package cn.edu.nju.moon.consistency.ui;

import cn.edu.nju.moon.consistency.checker.ReadIncChecker;
import cn.edu.nju.moon.consistency.model.observation.constructor.FileBasicObservationConstructor;
import cn.edu.nju.moon.consistency.model.observation.constructor.IBasicObservationConstructor;
import cn.edu.nju.moon.consistency.model.observation.constructor.JepsenObservationConstructor;
import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.schedule.WeakSchedule;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "checker",
        mixinStandardHelpOptions = true,
        showDefaultValues = true,
        version = "checker 1.0",
        description = "Loads a read/write trace file and checks for PRAM consistency.")
class Main implements Callable<Integer> {

    @CommandLine.Option(names = {"-v", "--viz"}, description = "Enable GraphViz visualization")
    private boolean enableVisualization = false;

    @CommandLine.Option(names = {"-f", "--format"}, description = "Trace file format", defaultValue = "Jepsen")
    private TraceFormat format = TraceFormat.Jepsen;

    @CommandLine.Parameters(index = "0", description = "Trace file to check")
    private String path;

    @Override
    public Integer call() throws Exception {
        GlobalData.VISUALIZATION = enableVisualization;

        IBasicObservationConstructor fcons;
        switch (format) {
            case Basic -> fcons = new FileBasicObservationConstructor(path);
            case Jepsen -> fcons = new JepsenObservationConstructor(path);
            default -> throw new IllegalStateException("Unexpected value: " + format);
        }

        var bob = fcons.construct();
        var checker = new ReadIncChecker(bob, fcons.get_ob_id(), new WeakSchedule(bob.getProcNum()));
        var consistent = checker.check();

        System.err.println(consistent ? "[✓] PRAM consistent" : "[✗] Not PRAM consistent");
        return consistent ? 0 : 1;
    }

    public static void main(String[] args) {
        var exitCode = new CommandLine(new Main())
                .setExitCodeExceptionMapper(new ExitCodeMapper())
                .execute(args);

        System.exit(exitCode);
    }

    enum TraceFormat {
        Basic,
        Jepsen
    }

    /**
     * Maps all unhandled exceptions to exit code 255 instead of 1
     */
    static class ExitCodeMapper implements CommandLine.IExitCodeExceptionMapper {
        @Override
        public int getExitCode(Throwable throwable) {
            return -1;
        }
    }
}