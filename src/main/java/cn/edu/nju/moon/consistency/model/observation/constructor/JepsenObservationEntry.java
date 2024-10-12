package cn.edu.nju.moon.consistency.model.observation.constructor;

import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.operation.BasicOperation;
import cn.edu.nju.moon.consistency.model.operation.RawOperation;
import us.bpsm.edn.Keyword;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static us.bpsm.edn.Keyword.newKeyword;

class JepsenObservationEntry {
    private Long index;
    private Long time;
    private String type;
    private Long process;
    private String function;
    private String key;
    private Long value;

    public JepsenObservationEntry(Object entry) {
        ParseFields((Map<?, ?>) entry);
    }

    private void ParseFields(Map<?, ?> m) {
        index = (Long) m.get(newKeyword("index"));
        time = (Long) m.get(newKeyword("time"));
        type = ((Keyword) m.get(newKeyword("type"))).getName();
        if (m.get(newKeyword("process")) instanceof Long)
            process = (Long) m.get(newKeyword("process"));
        function = ((Keyword) m.get(newKeyword("f"))).getName();

        List<?> valueList = null;
        if (m.get(newKeyword("value")) instanceof List<?>)
            valueList = (List<?>) m.get(newKeyword("value"));

        if (valueList != null && !valueList.isEmpty())
            key = valueList.get(0).toString();

        if (valueList != null && valueList.size() > 1 && valueList.get(1) instanceof Long)
            value = (Long) valueList.get(1);
    }

    public boolean isValidReadWriteCompletion() {
        if (process == null || key == null || value == null)
            return false;

        // write completion: consider succeeded writes, but also unconfirmed ones that might have succeeded
        if (function.equals("write") && (type.equals("ok") || type.equals("info")))
            return true;

        // read completion: only successfully read values are relevant
        return function.equals("read") && type.equals("ok");
    }

    public Optional<BasicOperation> tryCreateReadWriteOperation() {
        if (!isValidReadWriteCompletion())
            return Optional.empty();

        var rawOp = new RawOperation(
                function.equals("read") ? GlobalData.READ : GlobalData.WRITE,
                key,
                value.intValue());

        return Optional.of(new BasicOperation(rawOp));
    }

    public Long getIndex() {
        return index;
    }

    public Long getTime() {
        return time;
    }

    public Long getProcess() {
        return process;
    }
}
