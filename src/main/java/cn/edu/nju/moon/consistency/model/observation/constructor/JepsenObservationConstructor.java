package cn.edu.nju.moon.consistency.model.observation.constructor;

import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.observation.BasicObservation;
import cn.edu.nju.moon.consistency.model.process.BasicProcess;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

public class JepsenObservationConstructor implements IBasicObservationConstructor {

    private String filePath = null;
    private String observationId = null;

    public JepsenObservationConstructor(String filePath) {
        this.filePath = filePath;
        GlobalData.VARSET = new HashSet<String>();
    }

    @Override
    public BasicObservation construct() {
        var file = new File(filePath);
        this.observationId = file.getName();
        var bob = readEntries(file);
        insertMissingPids(bob);
        return bob;
    }

    private static BasicObservation readEntries(File file) {
        var bob = new BasicObservation();

        try {
            var reader = new BufferedReader(new FileReader(file));
            var p = Parsers.newParser(defaultConfiguration());
            var pbr = Parsers.newParseable(reader);

            Object value;
            while ((value = p.nextValue(pbr)) != Parser.END_OF_INPUT) {
                var entry = new JepsenObservationEntry(value);
                var op = entry.tryCreateReadWriteOperation();
                if (op.isEmpty())
                    continue;

                var pid = entry.getProcess().intValue();
                var proc = bob.getProcess(pid);
                if (proc == null) {
                    proc = new BasicProcess(pid);
                    bob.addProcess(pid, proc);
                }

                proc.addOperation(op.get());
            }

            reader.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return bob;
    }

    private static void insertMissingPids(BasicObservation bob) {
        if (bob.getProcMap().isEmpty())
            return;

        int maxPid = bob.getProcMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        for (var i = 0; i < maxPid; i++) {
            if (bob.getProcess(i) == null)
                bob.addProcess(i, new BasicProcess(i));
        }
    }

    @Override
    public String get_ob_id() {
        return this.observationId;
    }
}

