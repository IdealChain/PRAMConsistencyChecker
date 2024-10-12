package cn.edu.nju.moon.consistency.model.observation.constructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;

import cn.edu.nju.moon.consistency.model.GlobalData;
import cn.edu.nju.moon.consistency.model.observation.BasicObservation;
import cn.edu.nju.moon.consistency.model.operation.BasicOperation;
import cn.edu.nju.moon.consistency.model.process.BasicProcess;

/**
 * construct {@link BasicObservation} from file
 *
 * @author hengxin
 * @date 2012-12-8
 */
public class FileBasicObservationConstructor implements IBasicObservationConstructor
{
	private String fileName = null;
	private String ob_id = null;	// id of generated observation
	
	/**
	 * construct {@link BasicObservation} from file
	 * 
	 * @param fileName file to be read
	 */
	public FileBasicObservationConstructor(String fileName)
	{
		this.fileName = fileName;
		GlobalData.VARSET = new HashSet<String>();
	}

	/**
	 * construct {@link BasicObservation} from file
	 */
	@Override
	public BasicObservation construct()
	{
		File file = new File(fileName);
		this.ob_id = file.getName();
		
		BufferedReader reader = null;

		String procStr = null;
		StringTokenizer st = null;
		String token = null;

		BasicObservation bob = null;
		try
		{
			reader = new BufferedReader(new FileReader(file));

			bob = new BasicObservation();
			int pid = 0;
			while((procStr = reader.readLine()) != null)
			{
				BasicProcess proc = new BasicProcess(pid);
				st = new StringTokenizer(procStr);
				while(st.hasMoreTokens())
				{
					token = st.nextToken();
					BasicOperation bop = new BasicOperation(token);
					proc.addOperation(bop);
				}
				bob.addProcess(pid, proc);
				pid++;
			}
			reader.close();
		}
		catch(IOException ioe)
		{
			throw new RuntimeException(ioe);
		}

		return bob;
	}

	@Override
	public String get_ob_id()
	{
		return this.ob_id;
	}

}
