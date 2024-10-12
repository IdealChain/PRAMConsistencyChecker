package cn.edu.nju.moon.consistency.model.observation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import cn.edu.nju.moon.consistency.checker.ReadIncChecker;
import cn.edu.nju.moon.consistency.model.observation.constructor.FileBasicObservationConstructor;
import cn.edu.nju.moon.consistency.model.operation.BasicOperation;
import cn.edu.nju.moon.consistency.model.operation.ClosureOperation;
import cn.edu.nju.moon.consistency.model.operation.RawOperation;
import cn.edu.nju.moon.consistency.model.operation.ReadIncOperation;
import cn.edu.nju.moon.consistency.model.process.BasicProcess;
import cn.edu.nju.moon.consistency.model.process.ReadIncProcess;

/**
 * @author hengxin
 * @date 2012-12-6
 * 
 * @description raw observation is a collection of RawProcess
 * @modified hengxin on 2013-1-8
 * @reason refactor: no RawObservation any more; change to  
 */
public class BasicObservation
{
	/** {@link BasicProcess} with masterPid is to be checked against PRAM consistency **/
	protected int masterPid = -1;
	
	protected int procNum = -1;	/** number of processes */
	protected Map<Integer, BasicProcess> procMap = new HashMap<Integer, BasicProcess>();
	
	protected int totalOpNum = -1;
	protected Map<String, BasicOperation> write_pool = new HashMap<String, BasicOperation>();
	
	/**
	 * Default constructor: used in {@link FileBasicObservationConstructor}
	 * 	without {@link NullPointerException}
	 */
	public BasicObservation()
	{
		
	}
	
	/**
	 * Constructor:
	 * 	(1) set {@link #procNum}
	 *  (2) initialize {@link #procMap} to avoid {@link NullPointerException}
	 * @param procNum number of processes in the observation
	 */
	public BasicObservation(int procNum)
	{
		this.procNum = procNum;
		for (int pid = 0; pid < this.procNum; pid++)
			this.procMap.put(pid, new BasicProcess(pid));
	}
	
	/**
	 * add process into this observation
	 * @param pid assign pid to this process to add
	 * @param process process to add
	 */
	public void addProcess(int pid, BasicProcess process)
	{
		this.procMap.put(pid, process);
	}
	
	/**
	 * append the @param op to the RawProcess with @param pid
	 * 
	 * @param pid id of RawProcess
	 * @param op BasicOperation to be added
	 */
	public void addOperation(int pid, BasicOperation op)
	{
		BasicProcess proc = this.procMap.get(pid);
		if(proc == null)
			proc = new BasicProcess(pid);
		proc.addOperation(op);
		
		this.procMap.put(pid, proc);
	}
	
	/**
	 * preprocessing the {@link ReadIncObservation}, including
	 * (1) establishing "program order" between {@link ReadIncOperation}
	 * (2) establishing "write to order" between {@link ReadIncOperation}
	 */
	public void preprocessing()
	{
		this.establishProgramOrder();
		this.establishWritetoOrder();
	}
	
	/**
	 * does some READ {@link ReadIncOperation} read value from later WRITE
	 * {@link ReadIncOperation} on the same {@link ReadIncProcess} with masterPid
	 * 
	 * @return true, if some READ {@link ReadIncOperation} read value from later WRITE
	 * 	{@link ReadIncOperation} on the same {@link ReadIncProcess} with masterPid;
	 * 		   false, o.w..
	 * 
	 * @see ReadIncProcess#readLaterWrite()
	 * @see ReadIncChecker#check_part()
	 */
	public boolean readLaterWrite()
	{
		return this.procMap.get(this.masterPid).readLaterWrite();
	}
	
	/**
	 * is there no {@link ReadIncOperation} at all in the {@link ReadIncProcess}
	 * with masterPid. if it is the case, PRAM consistency is satisfied trivially.
	 * 
	 * @return true, if no {@link ReadIncOperation} 
	 * 	in {@link ReadIncProcess} with masterPid;	false, o.w..
	 */
	public boolean nullCheck()
	{
		try{
		if (this.procMap
				.get(this.masterPid)
				.getOpNum() == 0)
			return true;}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println(procMap.toString() + "\n" + this.masterPid);
		}
		return false;
	}
	
	/**
	 * establishing "program order" between {@link ClosureOperation}s
	 */
	protected void establishProgramOrder()
	{
		for (int pid : this.procMap.keySet())
			this.procMap.get(pid).establishProgramOrder();
	}
	
	/**
	 * establishing "write to order" between {@link ClosureOperation}s 
	 * and set rid for READ {@link ClosureOperation} and wid for corresponding
	 * WRITE {@link ClosureOperation} 
	 */
	protected void establishWritetoOrder()
	{
		// all READ {@link BasicOperation}s are in the {@link BasicProcess} with #masterPid
		this.procMap.get(this.masterPid).establishWritetoOrder(this);
	}
	
	/**
	 * process with pid
	 * @param pid id of process
	 * @return process with @param pid
	 */
	public BasicProcess getProcess(int pid)
	{
		return this.procMap.get(pid);
	}
	
	/**
	 * @return field {@link #procMap}
	 */
	public Map<Integer, BasicProcess> getProcMap()
	{
		return this.procMap;
	}
	
	/**
	 * @return number of processes in the observation: {@link #procNum}
	 */
	public int getProcNum()
	{
		/** in case the {@link BasicObservation} is initialized by the default
		 * constructor without @param procNum 
		 */
		if (this.procNum == -1)
			this.procNum = this.procMap.size();
		return this.procNum;
	}
	
	/**
	 * @return {@link #masterPid}
	 */
	public int getMasterPid()
	{
		return this.masterPid;
	}
	
	/**
	 * @return total number of operations
	 */
	public int getTotalOpNum()
	{
		if (this.totalOpNum == -1)
		{
			this.totalOpNum = 0;
			for (BasicProcess proc : this.procMap.values())
				totalOpNum += proc.getOpNum();
		}
		
		return this.totalOpNum;
	}
	
	/** ############### BEGIN: {@link #write_pool} related ################ */
	
	/**
	 * store all WRITEs into {@link #write_pool}
	 * 
	 * now: only used in constructors of {@link ClosureObservation} 
	 * 	and {@link ReadIncObservation}
	 */
	protected void storeWrite2Pool()
	{
		for (int pid : this.procMap.keySet())
			for (BasicOperation bop : this.getProcess(pid).getOpList())
				if (bop.isWriteOp())
					this.write_pool.put(bop.toString(), bop);
	}
	
	/**
	 * @param opStr String form of WRITE operation to retrieve
	 * @return WRITE operation in {@link #write_pool} with key = @param opStr
	 */
	public BasicOperation getWrite(String opStr)
	{
		return this.write_pool.get(opStr);
	}
	
	/**
	 * get the dictating WRITE for some READ
	 * @param rop READ operation
	 * @return dictating WRITE for @param rop
	 */
	public BasicOperation getDictatingWrite(BasicOperation rop)
	{
		assert rop.isReadOp() : "Get dictating WRITE of READ";

		return this.getWrite(rop.toString().replaceFirst("r", "w"));
	}
	
	/**
	 * @return {@link #write_pool}
	 */
	public Map<String, BasicOperation> getWritePool()
	{
		return this.write_pool;
	}
	/** ############### END: {@link #write_pool} related ################ */

	
	/**
	 * @description record the {@link BasicObservation} generated randomly
	 * @date 2013-1-7
	 * 
	 * @param rob {@link BasicObservation} to record 
	 * 
	 *   For random observation: fileName maybe 
	 * 		this.random_id = this.procNum + "_" + this.varNum + "_" + 
	 *		this.valRange + "_" + this.opNum + "_" + new Random().nextInt();
	 */
	public void record(String fileName)
	{
		try
		{
			FileWriter fw = new FileWriter("data/randomtest/" + fileName + ".txt");
			BufferedWriter out = new BufferedWriter(fw);
			out.write(this.toString());
			out.close();
		}
		catch (IOException ioe)
		{
			System.err.println("Failure with storage of randomly generated observation");
			ioe.printStackTrace();
		}
	}
	
	/**
	 * @return String format of {@link BasicObservation}
	 * 	which is suitable to be stored in file
	 * 
	 * Format: 
	 * one line for each process
	 * operations in each process are separated by whitespace
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		for (int pid : this.procMap.keySet())
		{
			for (RawOperation rop : this.procMap.get(pid).getOpList())
				sb.append(rop.toString()).append(' ');
			sb.append('\n');
		}
		
		return sb.toString();
	}
}
