package beankeeper.examples.enumtype;

import java.util.List;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import beankeeper.examples.PersistenceUtil;

public class PersistenceUtilTest {
	@BeforeTest
	public void setup() {

	}
	@AfterTest
	public void cleanup() {
		List<TestBean> all = PersistenceUtil.all(TestBean.class);
		for (TestBean node : all) {
			PersistenceUtil.delete(node);
		}
	}
	@Test
	public void testGet() {
		TestBean n = PersistenceUtil.one(TestBean.class, "where id=?", "hello");
		assert null == n;
		TestBean node = new TestBean();
		node.setId("hello");
		node.setMode(Mode.MODEA);
		PersistenceUtil.save(node);
		TestBean one = PersistenceUtil.one(TestBean.class, "where id = ?",
				"hello");
		assert one.getMode()==Mode.MODEA;
	}

	@Test
	public void testSave() {
		// fail("Not yet implemented");
	}

}
