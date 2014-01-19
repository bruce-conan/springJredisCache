package springJredisCache;

import javolution.util.FastMap;
import javolution.util.FastTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.BinaryJedis;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author 石头哥哥 </br>
 *         Date:1/7/14</br>
 *         Time:9:47 PM</br>
 *         Package:com.dc.gameserver.extComponents.jredisCache</br>
 *         Comment： 基于 spring + jedis封装的 redis 缓存操作接口
 *         运行时异常，IO异常，销毁binaryJedis对象
 *         spring- data redis中，
 *         open-->>getResource()
 *         向对象池借用 binaryJedis对象；
 *         close--->returnBrokenResource(binaryJedis) or returnResource(binaryJedis)
 *         如果出现异常（runntime ，io），那么将销毁 binaryJedis对象，否则将其归还到对象池；
 */
@Service
public class JRedisCache implements JCache {

    private static final  Logger LOGGER= LoggerFactory.getLogger(JRedisCache.class);

    @Resource
    private JRedisPool jRedisPool;

    /**
     * 运行时异常，IO异常，销毁binaryJedis对象
     * @param ex
     * @param jRedisPool
     * @param binaryJedis
     */
    protected void coverException(Exception ex,JRedisPool jRedisPool,BinaryJedis binaryJedis){
        if (ex instanceof JRedisCacheException||ex instanceof IOException){
            jRedisPool.returnBrokenResource(binaryJedis); //销毁该对象
        }else {
            //否则归还到对象池中
            jRedisPool.returnResource(binaryJedis);
        }
    }
    /**
     * 获取 redis information
     * @return
     */
    @Override
    public String info(){
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            LOGGER.info(binaryJedis.toString());
            return binaryJedis.info();
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }finally {
            jRedisPool.returnResource(binaryJedis);
        }
        return null;
    }

    /**
     * @param key
     * @return
     * @throws JRedisCacheException
     */
    @Override
    public ArrayList<?> getList(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            if (LOGGER.isDebugEnabled()){
                LOGGER.info("open connection->>"+binaryJedis.toString());
            }
            return (ArrayList<?>) JRedisSerializationUtils.deserialize(binaryJedis.get(key.getBytes()));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }finally {
            if (LOGGER.isDebugEnabled()){
                LOGGER.info("close connection->>"+binaryJedis.toString());
            }
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
        return null;
    }

    /**
     * Remove an item from the cache
     *
     * @param key
     */
    @Override
    public void removeList(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.del(key.getBytes());
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }

    /**
     * @param key
     * @param list
     */
    @Override
    public void putList(String key, ArrayList<?> list)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.set(key.getBytes(), JRedisSerializationUtils.serialize(list));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }

    /**
     * @param key
     * @param fastMap
     */
    @Override
    public void putFastMap(String key, FastMap<?,?> fastMap)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.set(key.getBytes(), JRedisSerializationUtils.serialize(fastMap));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }  finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }

    
    /**
     * @param key
     * @return
     * @throws      JRedisCacheException
     */
    @Override
    public FastMap<?,?> getFastMap(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            return (FastMap<?, ?>) JRedisSerializationUtils.deserialize(binaryJedis.get(key.getBytes()));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }  finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
        return null;
    }

    /**
     * Remove an item from the cache
     *
     * @param key
     */
    @Override
    public void removeFastMap(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.del(key.getBytes());
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }  finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }
 
    /**
     * Get an item from the cache, nontransactionally
     *
     * @param key
     * @return the cached object or <tt>null</tt>
     * @throws JRedisCacheException
     */
    @Override
    public Serializable getObject(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            return (Serializable) JRedisSerializationUtils.deserialize(binaryJedis.get(key.getBytes()));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
        return null;
    }

    /**
     * Add an item to the cache, nontransactionally, with
     * failfast semantics
     *
     * @param key
     * @param value
     * @throws JRedisCacheException
     */
    @Override
    public void putObject(String key, Serializable value)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.set(key.getBytes(),JRedisSerializationUtils.serialize(value));
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }

    /**
     * Remove an item from the cache
     *
     * @param key
     */
    @Override
    public void removeObject(String key)  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            binaryJedis.del(key.getBytes());
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null) jRedisPool.returnResource(binaryJedis);
        }
    }

    @Override
    public FastTable<String> keys()  {
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            FastTable<String> keys = new FastTable<String>();
            Set<byte[]> list = binaryJedis.keys(String.valueOf("*").getBytes());
            for (byte[] bs : list) {
                keys.addLast(bs == null ? null : (String) JRedisSerializationUtils.deserialize(bs));
            }
            return keys;
        } catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        }finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
        return null;
    }

    /**
     *
     */
    @Override
    public void destroy() {
        FastTable<String> keys=keys();
        BinaryJedis binaryJedis=null;
        try {
            binaryJedis=jRedisPool.getResource();
            for (String key:keys){
                //After the timeout the key will be
                // automatically deleted by the server.
                binaryJedis.expire(key.getBytes(),0);
            }
        }catch (Exception ex){
            coverException(ex,jRedisPool,binaryJedis);
        } finally {
            if (binaryJedis!=null)jRedisPool.returnResource(binaryJedis);
        }
    }

}