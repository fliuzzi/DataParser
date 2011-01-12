package com.where.places.lists;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.where.util.cache.ICache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

public class MemcacheUtil implements ICache {
	public static final int SPECIALCLIENT		= -3;
	public static final int LOCALCLIENT			= -2;
	public static final int RANDOMCLIENT		= -1;
	
	private static MemcacheUtil cache 			= new MemcacheUtil();  // dislike doing this.  but due to the fact we use SPRING this is the current workaround
	private static MemcachedClient[] clients;
	private static MemcachedClient localhostclient;
	private static MemcachedClient specialclient;
	private static int getWaitTime = 800;
	private static int cacheHoldTime = 60 * 60 * 24 * 7; // 7 days
	
	public MemcacheUtil() {}
	
	public void setHosts(String hosts) throws IOException {
		 setAllHosts(hosts);
	}
	
	public void setSpecial(String hosts) throws IOException {
		setSpecialClient(hosts);
	}
	
	public void setGetWait(String waittime) {
		setAllGetWait(waittime);
	}
	
	public void setCacheHoldTime(String holdtime) {
		setAllCacheHoldTime(holdtime);
	}
	
	public Object getObject(String id) {
		return get(id, "CSListingDocument");
	}
	
	public void setObject(String id, Object value) {
		set(id,value,"CSListingDocument");
	}
	
	public static void setAllHosts(String hosts) throws IOException {
		if(clients != null) return;
		List<InetSocketAddress> hostlist = AddrUtil.getAddresses(hosts);
		if(hostlist == null || hostlist.isEmpty()) throw new NullPointerException("No Hosts Listed");
		clients = new MemcachedClient[hostlist.size()];
		for(int i=0;i<hostlist.size();i++) {
			clients[i] = new MemcachedClient(hostlist.get(i));
			if(hostlist.get(i).getHostName().startsWith("localhost") ||
					hostlist.get(i).getHostName().startsWith("127.0.0.1")) localhostclient = clients[i];
		}
	}
	
	public static void setSpecialClient(String hosts) throws IOException {
		if(specialclient != null) return;
		specialclient = new MemcachedClient(AddrUtil.getAddresses(hosts));
	}
	
	public static void setAllGetWait(String waittime) {
		try {
			getWaitTime = Integer.parseInt(waittime);
		} catch (Exception e) {
			getWaitTime = 800;
		}
	}
	
	public static void setAllCacheHoldTime(String holdtime) {
		try {
			cacheHoldTime = Integer.parseInt(holdtime);
		} catch (Exception e) {
			cacheHoldTime = 60 * 60 * 24;
		}
	}
	
	public static MemcacheUtil getCache() { return cache; }
	
	public static MemcachedClient[] getClients() { return clients; }
	
	public static MemcachedClient getLocalhostClient() { return localhostclient; }
	
	public static MemcachedClient getSpecialClient() { return specialclient; }
	
	public static MemcachedClient getClient() {
		return getClient(LOCALCLIENT);
	}
	
	// -1 == random, -2 == random and prefer localhost, if greater or equal than size then -2, -3 == specialclient 
	public static MemcachedClient getClient(int ref) { 
		if(ref < SPECIALCLIENT || (clients != null && ref >= clients.length)) ref = LOCALCLIENT;
		if(ref == SPECIALCLIENT) {
			if(specialclient != null) return specialclient;
			else ref = LOCALCLIENT;
		}
		if(ref == LOCALCLIENT) {
			if(localhostclient != null) return localhostclient;
			else ref = RANDOMCLIENT;
		}
		if(clients == null || clients.length == 0) return null;
		if(ref == RANDOMCLIENT) return clients[(new Random()).nextInt(clients.length)];
		return clients[ref];
	}
	
	public static Object get(String key, Object sampletype) {
		return get(key, sampletype.getClass());
	}
	
	public static Object get(String key, Class<?> type) {
		return get(key, type.getName());
	}
	
	public static Object get(String key, String prefix) {
		return get(key, prefix, LOCALCLIENT);
	}
	
	public static Object get(String key, String prefix, int ref) {
		MemcachedClient client = getClient(ref);
		if(client == null) return null;
		Object val = null;
		Future<Object> f = null;
		try {
			prefix = clean(prefix);
			key = clean(key);
			f = client.asyncGet(prefix+"_"+key);
			val = f.get(getWaitTime, TimeUnit.MILLISECONDS);
		} catch (Throwable e) {
			if(e != null) f.cancel(false);
		} 
		return val;
	}
	
	public static Object getSync(String key, Object sampletype) {
		return getSync(key,sampletype.getClass());
	}
	
	public static Object getSync(String key, Class<?> type) {
		return getSync(key, type.getName());
	}
	
	public static Object getSync(String key, String prefix) {
		return getSync(key, prefix, LOCALCLIENT);
	}
	
	public static Object getSync(String key, String prefix, int ref) {
		MemcachedClient client = getClient(ref);
		if(client == null) return null;
		try {
			prefix = clean(prefix);
			key = clean(key);
			return client.get(prefix+"_"+key);
		} catch (Throwable t) {}
		return null;
	}
	
	public static void delete(String key, Object sampletype) {
		delete(key,sampletype.getClass());
	}
	
	public static void delete(String key, Class<?> type) {
		delete(key, type.getName());
	}
	
	public static void delete(String key, String prefix) {
		prefix = clean(prefix);
		key = prefix+"_"+clean(key);
		if(localhostclient != null) try { localhostclient.delete(key); } catch (Throwable t) {}
		if(specialclient != null) try { specialclient.delete(key); } catch (Throwable t) {}
		if(clients == null || clients.length == 0) return;
		for(MemcachedClient client: clients) {
			if(client != localhostclient) {
				try {
					client.delete(key);
				} catch (Throwable t) {}
			}
		}
	}
	
	public static void flush() {
		if(localhostclient != null) try { localhostclient.flush(); } catch (Throwable t) {}
		if(specialclient != null) try { specialclient.flush(); } catch (Throwable t) {}
		if(clients == null || clients.length == 0) return;
		for(MemcachedClient client: clients) {
			try {
				client.flush();
			} catch (Throwable t) {}
		}
	}
	
	public static void set(String key, Object value) {
		set(key, value, value.getClass().getName());
	}
	
	public static void set(String key, Object value, String prefix) {
		prefix = clean(prefix);
		key = prefix+"_"+clean(key);
		if(localhostclient != null) try { localhostclient.set(key, cacheHoldTime, value); } catch (Throwable t) {}
		if(specialclient != null) try { specialclient.set(key, cacheHoldTime, value); } catch (Throwable t) {}
		if(clients == null || clients.length == 0) return;
		for(MemcachedClient client: clients) {
			if(client != localhostclient) {
				try { 
					client.set(key, cacheHoldTime, value); 
				} catch (Throwable e) {}
			}
		}
	}
	
	public static String keyPrefix(Object sampletype) {
		return sampletype.getClass().getName();
	}
	
	public static String clean(String s) {
		try {
			if(s == null) s = "null";
			else {
				s = s.trim();
				s = s.replace(" ","");
				s = s.replaceAll("[\\p{Punct}]", "_");
			}
		} catch (Throwable t) {	}
		return s;
	}
}