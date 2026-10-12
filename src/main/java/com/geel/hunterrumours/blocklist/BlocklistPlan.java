package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public final class BlocklistPlan
{
    private final List<Row> rows;
    private final boolean complete;
    private final String nextAction;

    public List<Row> getRows()
    {
        return Collections.unmodifiableList(rows);
    }

    @Getter
    @AllArgsConstructor
    public static final class Row
    {
        public enum Status { COMPLETE, UNKNOWN, NEEDS_REROLL, ACTIVE }

        private final Hunter hunter;
        private final Rumour desired;
        private final Rumour observed;
        private final Status status;
    }
}
