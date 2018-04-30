package org.isouth.llt.db;

import org.isouth.llt.bootstrap.OnPropertyCondition;

/**
 * Start embedded maria db if condition matches
 *
 * @author qiyi
 * @since 1.0
 */
public class DBCondition extends OnPropertyCondition {
    public DBCondition() {
        super("llt.db.enable", false);
    }
}
