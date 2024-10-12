package cn.edu.nju.moon.consistency.checker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import cn.edu.nju.moon.consistency.datastructure.GlobalActiveWritesMap;
import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.observation.BasicObservation;
import cn.edu.nju.moon.consistency.model.observation.ReadIncObservation;
import cn.edu.nju.moon.consistency.model.operation.BasicOperation;
import cn.edu.nju.moon.consistency.model.operation.RawOperation;
import cn.edu.nju.moon.consistency.model.operation.ReadIncOperation;
import cn.edu.nju.moon.consistency.model.process.ReadIncProcess;
import cn.edu.nju.moon.consistency.schedule.ISchedule;
import cn.edu.nju.moon.consistency.ui.DotUI;

/**
 * @author hengxin
 * 
 * @modified hengxin on 2013-1-5
 * @reason 	 using Template Method design pattern
 * @see 	 Checker
 */
public class ReadIncChecker extends Checker
{
	private ReadIncObservation riob = null;	/** {@link ReadIncObservation} with respect to some process to check **/
	
	/**
	 * Constructor
	 * @param riob {@link BasicObservation} to check
	 */
	public ReadIncChecker(BasicObservation rob)
	{
		super(rob);
	}
	
	/**
	 * Constructor
	 * @param riob	{@link BasicObservation} to check
	 * @param name	for {@link DotUI}; the name of file for visualization
	 */
	public ReadIncChecker(BasicObservation rob, String name)
	{
		super(rob, name);
	}
	
	/**
	 * Constructor
	 * @param riob	{@link BasicObservation} to check
	 * @param name	for {@link DotUI}; the name of file for visualization
	 * @param s 	record of checking results
	 */
	public ReadIncChecker(BasicObservation rob, String name, ISchedule s)
	{
		super(rob, name, s);
	}
	
	/**
	 * @return {link ReadIncObservation} with respect to @param masterPid to check
	 */
	@Override
	protected BasicObservation getMasterObservation(int masterPid)
	{
		return new ReadIncObservation(masterPid, super.rob);
	}
	
	/**
	 * check whether {@link #riob} satisfies PRAM Consistency:
	 * 
	 * @return true; if {@link #riob} satisfies PRAM Consistency; false, otherwise.
	 */
	@Override
	protected boolean check_part(BasicObservation rob)
	{
		assert rob instanceof ReadIncObservation : "check ReadIncObservation";
		this.riob = (ReadIncObservation) rob;
		
		ReadIncProcess master_proc = this.riob.getMasterProcess();
		int master_size = master_proc.getOpNum();
		ReadIncOperation master_cur_rriop = null;
		BasicOperation bop = null;	
		boolean consistent = true;
		
		for (int index = 0; index < master_size; index++)
		{
			bop = master_proc.getOperation(index);
			if (bop.isReadOp())	// "ReadIncremental" checking algorithm is READ centric.
			{
				master_cur_rriop = (ReadIncOperation) bop;
				master_proc.set_cur_rriop(master_cur_rriop);	// set the current {@link ReadIncOperation} to check
				
				// (1) compute global active WRITEs
				boolean dominated = this.compute_globalActiveWritesMap(master_proc, master_proc.get_pre_rriop(), master_cur_rriop);
				// ui: for GAWM after computing intervals
				DotUI.getInstance().addGAWM(this.riob.getGlobalActiveWritesMap(), master_cur_rriop.toString(), 1);
				
				// (2) master_cur_rriop must read value from dw; apply Rule (c): W'WR order
				/**
				 * @modified hengxin on 2013-1-9
				 * @reason   if no other WRITEs than dw itself, it is not necessary to reschedule at all
				 */
				ReadIncOperation dw_cur_rriop = (ReadIncOperation) master_cur_rriop.getReadfromWrite();
				if (this.reschedule_assertion(master_cur_rriop, dw_cur_rriop))
				{
					if (this.readFromDW(master_cur_rriop, dw_cur_rriop))
					{
						consistent = false;	/** cycle **/
						break;
					}
					
					// (3) reschedule operations in r'-downset (i.e., master_pre_rriop-downset)
					if (dominated)
						if (this.reschedule(master_cur_rriop))
						{
							consistent = false;	/** cycle **/
							break;
						}
				}
				// ui: for GAWM after rescheduling (even if no rescheduling at all: for test)
				DotUI.getInstance().addGAWM(this.riob.getGlobalActiveWritesMap(), master_cur_rriop.toString(), 2);
				
				master_proc.advance_pre_rriop(master_cur_rriop);	// iterate over the next (R,R) pair
			}
		}
		
		// ui
		DotUI.getInstance().execute("readinc/" + name + "_" + master_proc.getPid());
		
		return consistent;	/** no cycle; satisfying PRAM Consistency **/
	}

	/**
	 * computation of global active WRITEs map: 
	 * 		{@link GlobalActiveWritesMap} in {@link ReadIncObservation}
	 *  
	 * @param master_proc {@link ReadIncProcess} to be checked; it is the process with master_pid
	 * @param master_pre_rriop previous READ {@link ReadIncOperation} 
	 * @param master_cur_rriop current READ {@link ReadIncOperation} in consideration
	 * @return true, if "dw" is in master_pre_rriop-downset; false, otherwise.
	 * 
	 * @constraints both @param master_pre_rriop and @param master_cur_rriop must be READ {@link ReadIncOperation}
	 * 
	 * NOTE: the returned value indicates whether "dw" is in master_pre_rriop-downset;
	 * 	it is by-product of this method
	 */
	private boolean compute_globalActiveWritesMap(ReadIncProcess master_proc, ReadIncOperation master_pre_rriop, ReadIncOperation master_cur_rriop)
	{
		assert master_pre_rriop.isReadOp() && master_cur_rriop.isReadOp() : "READ incremental: two arguments should be READ";
		
		boolean dominated = false;
		
		int master_pre_rindex = master_pre_rriop.getIndex();
		int master_cur_rindex = master_cur_rriop.getIndex();
		
		// (1) dealing with rr_interval: (master_pre_rriop, master_cur_rriop)
		ReadIncOperation pre_riop = master_pre_rriop;
		ReadIncOperation rr_wriop = null;	/* WRITE {@link ReadIncOperation} in rr_interval*/
		
		/** rr_interval: (master_pre_rriop, master_cur_rriop) **/
		for (int rr_index = master_pre_rindex + 1; rr_index < master_cur_rindex; rr_index++)	
		{
			rr_wriop = (ReadIncOperation) master_proc.getOperation(rr_index);
			assert rr_wriop.isWriteOp() : "WRITE ReadIncOperation in rr_interval";
			
			rr_wriop.getEarliestRead().initEarlistRead(master_cur_rriop);	// initialize earliest READ
			rr_wriop.getLatestWriteMap().updateLatestWrite(pre_riop);	// update latest WRITE map depending on previous operation
			this.riob.getGlobalActiveWritesMap().replace(rr_wriop);	// deactivate some WRITEs
			
			pre_riop = rr_wriop;	// iterate over the next WRITE  
		}
		master_cur_rriop.getLatestWriteMap().updateLatestWrite(pre_riop);	// update latest WRITE map for @param master_cur_rriop individually
		
		// (2) dealing with ww_interval
		ReadIncOperation dw = (ReadIncOperation) master_cur_rriop.getReadfromWrite();	// dictating WRITE for @param master_cur_rriop
		int pid_master = master_cur_rriop.getPid();	// pid of current READ
		int pid_dw = dw.getPid();	// pid of dictating WRITE
		if (pid_dw == pid_master)	// r and D(r) are in the same process and thus ww_interval is empty
		{
			if (dw.getIndex() < master_pre_rriop.getIndex())	// D(r) is in r'-downset
				dominated = true;
		}
		else	// r and D(r) are in different processes
		{
			ReadIncProcess dw_proc = (ReadIncProcess) this.riob.getProcess(pid_dw);	// ReadIncProcess in which dictating WRITE resides
			ReadIncOperation dw_pre_wriop = dw_proc.get_pre_wriop();	// previous WRITE {@link ReadIncOperation}; changing in iteration
			assert dw_pre_wriop.isWriteOp() : "Previous ReadIncOperation in ReadIncProcess not with masterPid is WRITE";
			
			ReadIncOperation ww_wriop = null;	// WRITE {@link ReadIncOperation} in ww_interval
			int dw_index = dw.getIndex();
			int dw_pre_wriop_index = dw_pre_wriop.getIndex();
			
			/** dealing with ww_interval: (dw_pre_wriop_index, dw_index] **/
			Map<String, ReadIncOperation> last_wriop_map = new HashMap<String, ReadIncOperation>();	/** record the last {@link ReadIncOperation} for each variable **/
			for (int ww_index = dw_pre_wriop_index + 1; ww_index <= dw_index; ww_index++)
			{
				ww_wriop = (ReadIncOperation) dw_proc.getOperation(ww_index);
				assert ww_wriop.isWriteOp() : "WRITE ReadIncOperation in ww_interval";
				
				ww_wriop.getEarliestRead().initEarlistRead(master_cur_rriop);	/** initialize earliest READ **/
				ww_wriop.getLatestWriteMap().updateLatestWrite(dw_pre_wriop);	/** update LatestWriteMap depending on previous operation **/
				
				/** 
				 * @description deactivate some WRITEs 
				 * @modified 	hengxin on 2013-1-6
				 * @reason   	there are two reasons to deactivate WRITEs:
				 * 			(1) due to dw_pre_wriop (with corresponding READs)
				 * 			(2) due to ww_interval itself (mainly focus on ones without corresponding READs)
				 */
				this.riob.getGlobalActiveWritesMap().deactivateFrom(dw_pre_wriop, ww_wriop.getVariable());	// deactivate some WRITE due to dw_pre_wriop
//				this.riob.getGlobalActiveWritesMap().deactivateFrom(dw_pre_wriop_constant);  /** deactivate some WRITE due to dw_pre_wriop **/
//				this.riob.getGlobalActiveWritesMap().addActiveWrite(ww_wriop);	/** add this new active WRITE **/
				last_wriop_map.put(ww_wriop.getVariable(), ww_wriop);	/** record the last {@link ReadIncOperation} for this variable **/
				
				dw_pre_wriop = ww_wriop;	// iterate over the next WRITE
			}
			this.riob.getGlobalActiveWritesMap().addActiveWriteMap(last_wriop_map);	/** add active WRITE for each variable **/
			master_cur_rriop.getLatestWriteMap().updateLatestWrite(dw_pre_wriop);
			
			if (dw_index <= dw_pre_wriop_index)	// r and D(r) are in different processes and D(r) is in r'-downset
				dominated = true;
			else
				dw_proc.advance_pre_wriop(dw_pre_wriop);	// advance the previous WRITE forward to the new one
		}
//		/**
//		 *  (3) dealing with @param master_cur_rriop and "dw" separately and specially:
//		 *  	@param master_cur_rriop reads value from "dw", causing other WRITEs with
//		 *  	the same variable are scheduled before "dw" and LatestWrite are updated 
//		 *  	accordingly 
//		 */
//		ReadIncOperation temp_riop = new ReadIncOperation(new RawOperation(GlobalData.WRITE, GlobalData.DUMMYVAR, -1));	// temp 
//		for (ReadIncOperation active_wriop : this.riob.getGlobalActiveWritesMap().getActiveWrites(master_cur_rriop.getVariable()))
//			temp_riop.getLatestWriteMap().updateLatestWrite(active_wriop);
		
		return dominated;
	}
	
	/**
	 * is it necessary to reschedule operations?
	 * There are two cases in which no reschedule is necessary at all:
	 * (1) D(R) has not been overwritten, and there exist some WRITE other than D(R) 
	 * (2) D(R) has been overwritten, and there still exist some WRITE
	 * 
	 * @return true, if it is necessary to reschedule operations; false, otherwise.
	 * 
	 * @constraints @param dw_cur_rriop has the "writeto" relation with @param cur_rriop
	 * 
	 * @author hengxin
	 * @date 2013-1-9
	 */
	private boolean reschedule_assertion(ReadIncOperation cur_rriop, ReadIncOperation dw_cur_rriop)
	{
		assert cur_rriop.isReadOp() && dw_cur_rriop.isWriteOp()
				&& cur_rriop.getVariable().equals(dw_cur_rriop.getVariable())
				&& cur_rriop.getValue() == dw_cur_rriop.getValue() : "D(R) writes to R";
		
		Set<ReadIncOperation> gaws = this.riob.getGlobalActiveWritesMap().getActiveWrites(cur_rriop.getVariable());
		return ( (gaws.contains(dw_cur_rriop) && gaws.size() >= 2) || (! gaws.contains(dw_cur_rriop) && gaws.size() >= 1));
	}
	
	/**
	 * @description reschedule (i.e., enforce) orders among {@link ReadIncOperation}s   
	 * 
	 * @param master_cur_rriop current READ {@link ReadIncOperation} being checked			
	 * @return true, if cycle emerges; false, otherwise.
	 */
	private boolean reschedule(ReadIncOperation master_cur_rriop)
	{
		assert master_cur_rriop.isReadOp() : "Reschedule operations due to the current READ ReadIncOperation";
		
		ReadIncOperation dw = (ReadIncOperation) master_cur_rriop.getReadfromWrite();	// dictating WRITE {@link ReadIncOperation}
		Set<ReadIncOperation> candidateSet = this.draw_reschedule_boundary(dw);	// identify the possible rescheduled operations
		
		Queue<ReadIncOperation> zeroQueue = new LinkedList<ReadIncOperation>();	// queue containing operations ready to check
		zeroQueue.offer(dw);	// check dw first
		while (! zeroQueue.isEmpty())
		{
			ReadIncOperation wprime_riop = zeroQueue.poll();
			
			/**
			 * @description rule out the case that wprime_riop is READ
			 * 
			 * @modified hengxin on 2013-1-7
			 * @reason wprime_riop maybe READ (e.g., R and D(R) are in the same process)
			 */
			if (wprime_riop.isWriteOp() && ! wprime_riop.isDone())
			{
				// identify the possible W'WR order
				int oldEarlistRead = wprime_riop.getEarliestRead().updateEarliestRead(wprime_riop.getSuccessors());
				ReadIncOperation wriop = wprime_riop.getEarliestRead().identify_wrPair(oldEarlistRead, wprime_riop, this.riob.getMasterProcess());
				
				// apply W'WR order: wprime_riop => wriop
				if (wriop != null)
					if (wprime_riop.apply_wprimew_order(wriop, this.riob))
						return true;	/** cycle **/
				
				// depending on UNDONE operation
				if (wriop != null && wriop.isCandidate() && ! wriop.isDone())
				{
					
//					if (! wriop.isDone())
//					{
						/**
						 * @modified hengxin on 2013-1-9
						 * @modification the update of predecessors and successors 
						 *   are encapsulated in methods related to different kinds of edges
						 * @reason maintenance of predecessors and successors
						 */
//						wprime_riop.getSuccessors().add(wriop);
//						wriop.getPredecessors().add(wprime_riop);
						wprime_riop.incCount();
//					}
//					else
//					{
//						wprime_riop.getEarliestRead().updateEarliestRead(wriop);
//					}
				}
			}
			
			/**
			 * delete dependency and identify operations ready to check
			 * no matter whether it is READ or WRITE 
			 */
			if (wprime_riop.getCount() == 0)
			{
				wprime_riop.setDone();
				
				ReadIncOperation tmp_riop = null;
				for (BasicOperation bop : wprime_riop.getPredecessors())
				{
					tmp_riop = (ReadIncOperation) bop;
					tmp_riop.decCount();
					if (tmp_riop.getCount() == 0 && ! tmp_riop.isDone())	/** TODO is it necessary to check riop.isDone() **/
						zeroQueue.offer(tmp_riop);
				}
			}
		}
		
		/**
		 * @modified hengxin on 2013-1-5
		 */
		for (ReadIncOperation riop : candidateSet)	/** reset flag isCandidate **/
			riop.resetCandidate();
		
		return false;	/** no cycle **/
	}
	
	/**
	 * applying Rule (c): W'WR order due to the fact that 
	 * @param master_cur_rriop must read value from @param dw 
	 * 
	 * @param master_cur_rriop READ {@link ReadIncOperation}
	 * @param dw READ {@link ReadIncOperation}
	 * 
	 * @return true, if cycle emerges; false, otherwise.
	 * 
	 * @modified hengxin on 2013-1-4
	 * @reason you cannot remove elements [apply_wprimew_order] from ArrayList in for each syntax
	 * 
	 * @modified hengxin on 2013-1-9
	 * @modification remove the third parameter {@link ReadIncObservation}
	 * @reason you can assess {@link #riob} in this class directly
	 */
	private boolean readFromDW(ReadIncOperation master_cur_rriop, ReadIncOperation dw)
	{
		assert master_cur_rriop.isReadOp() && dw.isWriteOp() : "READ reads from some WRITE";
		
		String var = master_cur_rriop.getVariable();
		
		for (String wriopStr : this.riob.getGlobalActiveWritesMap().getActiveWritesPool(var))
		{
			ReadIncOperation wriop = (ReadIncOperation) this.riob.getWrite(wriopStr);
			if (! wriop.equals(dw) && wriop.apply_wprimew_order(dw,this.riob))	/** apply Rule (c): W'WR order (i.e., wriop => dw => master_cur_rriop) **/
				return true;	/** cycle **/
		}
		
		return false;  /** no cycle **/
	}
	
	/**
	 * @description  perform BFS on G^T from D(r) to identify the 
	 * possible rescheduled {@link ReadIncOperation}s
	 * 
	 * @modified hengxin on 2013-1-5
	 * @reason dealing with {@link ReadIncOperation#isCandidate}
	 * 
	 * @param {@link ReadIncOperation} from which the boundary is drawn
	 * @return set of {@link ReadIncOperation}s involved in the reschedule
	 */
	private Set<ReadIncOperation> draw_reschedule_boundary(ReadIncOperation wriop)
	{
		assert wriop.isWriteOp() : "perform DFS from D(r)";
		
		Set<ReadIncOperation> candidateSet = new HashSet<ReadIncOperation>();
		Queue<ReadIncOperation> pending = new LinkedList<ReadIncOperation>();	// pending queue for BFS framework
		wriop.initCount(0);		// the first operation to consider in reschedule (topological sorting) 
		pending.offer(wriop);	// enqueue the start operation
		while (! pending.isEmpty())
		{
			ReadIncOperation cur_op = pending.poll();	// it is a possible rescheduled operation
			cur_op.setCandidate();	// mark the possible rescheduled operation
			candidateSet.add(cur_op);	/** add it to set **/
			cur_op.resetDone();		// reset to be undone
			
			ReadIncOperation tmp_riop = null;
			for (BasicOperation bop: cur_op.getPredecessors())
			{
				tmp_riop = (ReadIncOperation) bop;
				tmp_riop.incCount();	// one more dependency operation
				if (! tmp_riop.isCandidate())
					pending.add(tmp_riop);
			}
			/**
			 *  @modified by hengxin 2013-01-03
			 */
//			pending.addAll(cur_op.getPredecessors());
		}
		
		return candidateSet;
	}
	
}
