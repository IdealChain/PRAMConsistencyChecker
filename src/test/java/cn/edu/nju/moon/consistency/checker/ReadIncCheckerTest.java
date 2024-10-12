package cn.edu.nju.moon.consistency.checker;

import org.junit.Before;
import org.junit.Test;

import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.observation.constructor.FileBasicObservationConstructor;
import cn.edu.nju.moon.consistency.model.observation.constructor.IBasicObservationConstructor;
import cn.edu.nju.moon.consistency.model.observation.constructor.RandomBasicObservationConstructor;
import cn.edu.nju.moon.consistency.schedule.constructor.RandomViewFactory;

public class ReadIncCheckerTest
{
	
	@Before
	public void setUp() throws Exception
	{
	}

	/**
	 * @description from Fig 4. Case 2b): R and D(R) are in the same process and D(R) is in R'-downset
	 * @author hengxin
	 * @date 2013-1-7 
	 */
//	@Test
	public void testCheck_part_fig4()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons_fig4_2b = new FileBasicObservationConstructor("src/test/resources/testset/obfig4case2b");
		Checker ri_checker_fig4_2b = new ReadIncChecker(frobcons_fig4_2b.construct(), frobcons_fig4_2b.get_ob_id());
		ri_checker_fig4_2b.check();
	}
	
	/**
	 * @description from Fig 5. Case 1b): R and D(R) are in different processes and D(R) is in R'-downset
	 * @author hengxin
	 * @date 2013-1-7
	 */
//	@Test
	public void testCheck_part_fig5()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons_fig5_1b = new FileBasicObservationConstructor("src/test/resources/testset/obfig5case1b");
		Checker ri_checker_fig5_1b = new ReadIncChecker(frobcons_fig5_1b.construct(), frobcons_fig5_1b.get_ob_id());
		ri_checker_fig5_1b.check();
	}
	
	/**
	 * @description figure 6 (multiple WRITE)
	 * @modified hengxin on 2013-1-5
	 * @reason refactor IChecker to Checker using Template Method design pattern
	 */
//	@Test
	public void testCheck_part_fig6()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons_fig6 = new FileBasicObservationConstructor("src/test/resources/testset/readinc/obfig6");
		Checker ri_checker_fig6 = new ReadIncChecker(frobcons_fig6.construct(), frobcons_fig6.get_ob_id());
		ri_checker_fig6.check();
	}
	
	/**
	 * @description figure 7 (cycle detection)
	 * @author hengxin
	 * @date 2013-1-5
	 */
//	@Test
	public void testCheck_part_fig7()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons_fig7 = new FileBasicObservationConstructor("src/test/resources/testset/obfig7");
		Checker ri_checker_fig7 = new ReadIncChecker(frobcons_fig7.construct(), frobcons_fig7.get_ob_id());
		ri_checker_fig7.check();
	}

	/**
	 * @description file: testset/readinc/obfigreschedulecycle
	 * @author hengxin
	 * @date 2013-1-22
	 */
//	@Test
	public void testCheck_part_reschedule_cycle()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/readinc/obfigreschedulecycle");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
	/**
	 * @description file: testset/readinc/inrounds
	 * @author hengxin
	 * @date 2013-1-23
	 */
//	@Test
	public void testCheck_part_inrounds()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/readinc/inrounds");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
	/**
	 * @description simple execution: wx1 wx2 ry1 rx1; wy1 
	 * @author hengxin
	 * @date 2013-1-9
	 */
//	@Test
	public void testCheck_part_wwprimer()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/readinc/wwprimer");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
	/**
	 * @description test (random observation)
	 * @author hengxin
	 * @date 2013-1-6
	 */
//	@Test
	public void testCheck_random()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor randcons_0 = new RandomBasicObservationConstructor(10, 8, 15, 200, new RandomViewFactory());
		Checker ri_checker_rand0 = new ReadIncChecker(randcons_0.construct(), randcons_0.get_ob_id());
		ri_checker_rand0.check();
	}
	
//	@Test
	public void testCheck_random_1549()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor randcons_0 = new FileBasicObservationConstructor("src/test/resources/testset/randomclosure/1549");
		Checker ri_checker_rand0 = new ReadIncChecker(randcons_0.construct(), randcons_0.get_ob_id());
		ri_checker_rand0.check();
	}

	
	/** %%%%%%%%%%%%%%%%%%%%% for joint tests %%%%%%%%%%%%%%%%%%%%%%%%%%% */
//	@Test
	public void testCheck_part_jt_50_420()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/readinc/jt_50_420");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
//	@Test
	public void testCheck_part_4_40()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/randomreadinc/4_40");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
//	@Test
	public void testCheck_part_4_40_1()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/randomreadinc/4_40_1");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
//	@Test
	public void testCheck_part_4_40_2()
	{
		GlobalData.VISUALIZATION = true;

		IBasicObservationConstructor frobcons = new FileBasicObservationConstructor("src/test/resources/testset/randomreadinc/4_40_2");
		Checker ri_checker = new ReadIncChecker(frobcons.construct(), frobcons.get_ob_id());
		ri_checker.check();
	}
	
}
