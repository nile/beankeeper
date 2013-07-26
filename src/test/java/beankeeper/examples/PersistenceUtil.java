package beankeeper.examples;

import hu.netmind.beankeeper.Store;

import java.util.List;

import org.apache.commons.beanutils.BeanUtils;


public class PersistenceUtil {
	private static Store store = null;

	public static Store getStore() {
		return store;
	}

	static {
		store = new Store("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:file:data/agentdb");
	}
	
	public static <T> T save(T record) {
		getStore().save(record);
		return record;
			
	}
	public static <T> boolean saveOrUpdate(T obj) {
		try {
			Object one = one(obj.getClass(),"where id = ?", BeanUtils.getProperty(obj, "id"));
			if(null != one) {
				BeanUtils.copyProperties(one, obj);
				store.save(one);
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		save(obj);
		return true;
	}
	public static <T> T delete(T t) {
		getStore().remove(t);
		return t;
	}
	@SuppressWarnings("unchecked")
	public static <T> T one(Class<T> clz,String where, Object... params) {
		Object find = getStore().findSingle("find " + JavaUtil.getClassName(clz) + " " + where, params);
		return (T) find;
	}
	public static <T> List<T> find(Class<T> clz,String where, Object... params) {
		@SuppressWarnings("unchecked")
		List<T> find = getStore().find("find " + JavaUtil.getClassName(clz) + " " + where, params);
		return find;
	}
	public static <T > T getById(Class<T> clz, Object id) {
		return one(clz,"where id = ?",id);
	}

	
	public static <T> List<T> all(Class<T> clz) {
		return find(clz,"");
	}
	private static class JavaUtil{
		public static String getClassName(Class<?> clz) {
			String simpleName = clz.getSimpleName();
			return simpleName.substring(0, 1).toLowerCase()+simpleName.substring(1);
		}
	}

}
