package cn.edu.nju.moon.consistency.model.operation;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import cn.edu.nju.moon.consistency.schedule.View;
import cn.edu.nju.moon.consistency.ui.DotUI;

/**
 * @author hengxin
 * @date 2012-12-6
 * 
 * @description basic operation which resides on some process
 * 	and thus has the field pid.
 */
public class BasicOperation extends RawOperation
{
	/**
	 * "pid" depend on the RawProcess 
	 * on which the BasicOperation reside
	 */
    private int pid = -1;
    
    /** index in process */
    private int index = -1;	
    
    /**
     * @modified hengxin on 2013-1-9
	 * @modification adding this flag for {@link View#View(BasicObservation)} 
     */
    private boolean isInView = false;
    
	/** 
	 * very basic "precede order"
	 */
	protected BasicOperation programOrder = null;		// program order
	protected BasicOperation reProgramOrder = null;		// reverse program order
	protected BasicOperation readfromOrder = null; 		// read from order
	protected List<BasicOperation> writetoOrder 
				= new ArrayList<BasicOperation>();	// writeto order
	
	public BasicOperation(RawOperation otherOp)
	{
		super(otherOp);
	}
	
	public BasicOperation(String opStr)
	{
		super(opStr);
	}
	
	/**
	 * @return {@link #pid}
	 */
	public int getPid()
	{
		return pid;
	}

	public void setPid(int pid)
	{
		this.pid = pid;
	}
	
	public void setIndex(int index)
	{
		this.index = index;
	}
	
	/**
	 * @return {@link #index}
	 */
	public int getIndex()
	{
		return this.index;
	}

	/*********** BEGIN: order related methods ************/

	/**
	 * set the next {@link BasicOperation} in the program order
	 * @param bop the next {@link BasicOperation} in the program order
	 */
	public void setProgramOrder(BasicOperation bop)
	{
		this.programOrder = bop;
		bop.reProgramOrder = this;
		
		// ui
		DotUI.getInstance().addPOEdge(this, bop);
	}
	
	/**
	 * @return the next operation in program order
	 */
	public BasicOperation getProgramOrder()
	{
		return this.programOrder;
	}

	/**
	 * set Writeto Order: this => @param rop
	 * @param rop READ operation in some Writeto Order 
	 */
	public void addWritetoOrder(BasicOperation rop)
	{
		assertTrue("WRITE writes to READ", this.isWriteOp() && rop.isReadOp());
		
		this.writetoOrder.add(rop);
		rop.readfromOrder = this;
		
		// ui
		DotUI.getInstance().addWritetoEdge(this, rop);
	}
	
	/**
	 * @return dictating WRITE {@link BasicOperation} from which this READ reads
	 * 
	 * @constraints this must be READ {@link BasicOperation}
	 */
	public BasicOperation getReadfromWrite()
	{
		assertTrue("READ reads from WRITE", this.isReadOp());
		
		return this.readfromOrder;
	}
	
	/**
	 * @return dictated READ {@link BasicOperation}s for this WRITE one
	 * 
	 * @constraints this must be WRITE {@link BasicOperation}
	 */
	public List<BasicOperation> getWritetoOrder()
	{
		assertTrue("WRITE writes to READ", this.isWriteOp());
		
		return this.writetoOrder;
	}
	
	/**
	 * @return {@link #isInView}
	 */
	public boolean isInView()
	{
		return this.isInView;
	}
	
	/**
	 * set {@link #isInView} true
	 */
	public void setInView()
	{
		this.isInView = true;
	}
	
	/**
	 * reset {@link #isInView} false
	 */
	public void resetInView()
	{
		this.isInView = false;
	}
	
	/**
	 * if the {@link BasicOperation}s are both READ, they may be differentiated from
	 * each other with respect to the field {@link #index}.
	 */
	@Override
	public boolean equals(Object obj)
	{
		boolean basicComparison = super.equals(obj);
		
		if (basicComparison && this.isReadOp())
			return this.index == ((BasicOperation) obj).index;
		
		return basicComparison;
	}
	
}
