package com.platform.shiro;

import com.alibaba.fastjson.JSON;
import com.platform.cache.MemCacheUtils;
import com.platform.utils.Constant;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class MemShiroSessionDAO extends EnterpriseCacheSessionDAO {

    private static Logger logger = LoggerFactory.getLogger(MemShiroSessionDAO.class);
    
    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = super.doCreate(session);

        final String key = Constant.SESSION_KEY + sessionId.toString();
        logger.info("MemShiroSessionDAO=doCreate$key={}$val={}", sessionId, JSON.toJSONString(session));
        setShiroSession(key, session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = super.doReadSession(sessionId);
        if (null == session) {
            final String key = Constant.SESSION_KEY + sessionId.toString();
            session = getShiroSession(key);
        }
        logger.info("MemShiroSessionDAO=doReadSession$key={}$val={}", sessionId, JSON.toJSONString(session));
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        super.doUpdate(session);
        final String key = Constant.SESSION_KEY + session.getId().toString();
        logger.info("MemShiroSessionDAO=doUpdate$key={}$val={}", session.getId(), JSON.toJSONString(session));
        setShiroSession(key, session);
    }

    @Override
    protected void doDelete(Session session) {
        super.doDelete(session);
        final String key = Constant.SESSION_KEY + session.getId().toString();
        logger.info("MemShiroSessionDAO=doRemove$key={}$val={}", session.getId(), JSON.toJSONString(session));
        MemCacheUtils.remove(key);
    }

    private Session getShiroSession(String key) {
        return (Session)MemCacheUtils.get(key);
    }

    private void setShiroSession(String key, Session session) {
        MemCacheUtils.put(key, session);
    }
}
