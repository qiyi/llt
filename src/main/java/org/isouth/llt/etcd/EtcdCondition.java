package org.isouth.llt.etcd;

import org.isouth.llt.bootstrap.OnPropertyCondition;

/**
 * Start embedded etcd if condition matches
 *
 * @author qiyi
 * @since 1.0
 */
public class EtcdCondition extends OnPropertyCondition {
    public EtcdCondition() {
        super("llt.etcd.enable", false);
    }
}
