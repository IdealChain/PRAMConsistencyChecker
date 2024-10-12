package cn.edu.nju.moon.consistency.model.observation;

import cn.edu.nju.moon.consistency.checker.Checker;
import cn.edu.nju.moon.consistency.checker.ReadIncChecker;
import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.observation.constructor.JepsenObservationConstructor;
import cn.edu.nju.moon.consistency.model.operation.RawOperation;
import cn.edu.nju.moon.consistency.schedule.WeakSchedule;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class JepsenObservationConstructorTest {
    private static final Path TraceDir = Paths.get("src", "test", "resources", "jepsen");

    @Before
    public void setUp() throws Exception {
        GlobalData.VISUALIZATION = false;
    }

    @Test
    public void TestDeleteEv() {
        assertEventual(getChecker(TraceDir.resolve("fifo_delete_ev.edn")));
    }

    @Test
    public void TestDeleteFifo() {
        assertFifo(getChecker(TraceDir.resolve("fifo_delete_fifo.edn")));
    }

    @Test
    public void TestGetEv() {
        assertEventual(getChecker(TraceDir.resolve("fifo_get_ev.edn")));
    }

    @Test
    public void TestGetFifo() {
        assertFifo(getChecker(TraceDir.resolve("fifo_get_fifo.edn")));
    }

    @Test
    public void TestPriorityEv() {
        assertEventual(getChecker(TraceDir.resolve("fifo_priority_ev.edn")));
    }

    @Test
    public void TestPriorityFifo() {
        assertFifo(getChecker(TraceDir.resolve("fifo_priority_fifo.edn")));
    }

    @Test
    public void TestSyncBuffersEv() {
        assertEventual(getChecker(TraceDir.resolve("fifo_syncbuffers_ev.edn")));
    }

    @Test
    public void TestSyncBuffersFifo() {
        assertFifo(getChecker(TraceDir.resolve("fifo_syncbuffers_fifo.edn")));
    }

    @Test
    public void TestSyncUpEv() {
        assertEventual(getChecker(TraceDir.resolve("fifo_syncup_ev.edn")));
    }

    @Test
    public void TestSyncUpFifo() {
        assertFifo(getChecker(TraceDir.resolve("fifo_syncup_fifo.edn")));
    }

    private Checker getChecker(Path filePath) {
        var fcons = new JepsenObservationConstructor(filePath.toString());
        var bob = fcons.construct();

        System.out.printf("%s: constructed observation with %d ops:%n%s%n",
                fcons.get_ob_id(), bob.getTotalOpNum(), printObservation(bob));

        return new ReadIncChecker(bob, fcons.get_ob_id(), new WeakSchedule(bob.getProcNum()));
    }

    public String printObservation(BasicObservation bob) {
        StringBuilder sb = new StringBuilder();

        for (int pid : bob.getProcMap().keySet()) {
            sb.append(pid);
            sb.append(':');
            for (RawOperation rop : bob.getProcess(pid).getOpList())
                sb.append(rop.toString()).append(' ');
            sb.append('\n');
        }

        return sb.toString();
    }

    private void assertEventual(Checker checker) {
        var consistent = checker.check();
        System.out.printf("PRAM consistent? %b%n", consistent);

        if (consistent)
            System.out.printf("%nSchedule:%n%s%n", checker.getSchedule());
    }

    private void assertFifo(Checker checker) {
        var consistent = checker.check();
        System.out.printf("PRAM consistent? %b%n", consistent);
        assertTrue("FIFO observation should be PRAM consistent", consistent);

        System.out.printf("%nSchedule:%n%s%n", checker.getSchedule());
    }
}