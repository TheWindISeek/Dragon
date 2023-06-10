package test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

/*指令
 * x=? 赋值
 * x++ x+1
 * x-- x-1
 * !?? 阻塞时间
 * end 结束进程
 * gob返回第一条指令
 * 
 * 
 * */
public class mainwindow {
	//public static mainwindow mw;
	static JFrame window;
	static JTextField ui_currentprocess;
	static JTextField ui_instruction;
	static JTextField ui_results;
	static JTextField ui_priority;
	static JTextField ui_timeusage;
	static JTextField ui_timeslice;
	
	static JList<String> ui_r_processname;
	static JList<String> ui_r_priority;
	static JList<String> ui_r_wait;
	static DefaultListModel<String> m_r_processname;
	static DefaultListModel<String> m_r_priority;
	static DefaultListModel<String> m_r_wait;
	
	static JList<String> ui_c_processname;
	static JList<String> ui_c_priority;
	static JList<String> ui_c_wait;
	static JList<String> ui_c_reason;
	static DefaultListModel<String> m_c_processname;
	static DefaultListModel<String> m_c_priority;
	static DefaultListModel<String> m_c_wait;
	static DefaultListModel<String> m_c_reason;
	
    static Pattern pattern = Pattern.compile("[^0-9]");

    static boolean INTP;//中断是否处理
	static int Slice = 5;//时间片
	static String IR;//指令
	
	static int PID;//PCB号
	static int PSW;//中断号
	static int INTR;//中断时间
	static int IP; //指令位置
	static int DR;//x的值
	static int TIME;//进程用时
	
	static PCB pcbcurrent;//当前进程控制块
	static int PCBindex;//当前进程序号
	static int PCBIDcount= 0;//新建进程id
	static int CPUclock = 0;//启动时间
	
	static String[] codesegment = new String[512];//代码段
	static int[] datasegment;//数据段
	static Vector<Integer[]> code_usage = new Vector<Integer[]>();//记录全局的代码段资源申请
	static Vector<Integer[]> data_usage = new Vector<Integer[]>();//记录全局的数据段资源申请
	static Vector<PCB> processque = new Vector<PCB>();//队列
	public static void main(String[] args)
	{
		//UI创建部分
		{
			window = new JFrame("可视化进程管理");
			window.setSize(450, 360);//设置大小
			window.setLocationRelativeTo(null);//设置居中
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置可关闭
			window.setLayout(new FlowLayout());//设置绝对布局（窗口里面的内容不会随着窗口的改变而改变
			window.setResizable(false);
			window.setVisible(true);//设置面板可见
			
			JPanel ui_upper = new JPanel();
			ui_upper.setLayout(new FlowLayout());
			ui_upper.setBorder(BorderFactory.createEtchedBorder());
			window.add(ui_upper);
			
			JPanel ui_lower = new JPanel();
			ui_lower.setLayout(new FlowLayout());
			window.add(ui_lower);
			
			JPanel temppanel;
			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_currentprocess = new JLabel("当前进程");
			ui_currentprocess = new JTextField("");
			ui_currentprocess.setBounds(100,100,100,50);
			ui_currentprocess.setEditable(false);
			temppanel.add(ui_t_currentprocess,BorderLayout.NORTH);
			temppanel.add(ui_currentprocess,BorderLayout.SOUTH);
			ui_upper.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_instruction = new JLabel("执行指令");
			ui_instruction = new JTextField("");
			ui_instruction.setBounds(100,100,100,50);
			ui_instruction.setEditable(false);
			temppanel.add(ui_t_instruction,BorderLayout.NORTH);
			temppanel.add(ui_instruction,BorderLayout.SOUTH);
			ui_upper.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_results = new JLabel("中间结果");
			ui_results = new JTextField("");
			ui_results.setBounds(100,100,100,50);
			ui_results.setEditable(false);
			temppanel.add(ui_t_results,BorderLayout.NORTH);
			temppanel.add(ui_results,BorderLayout.SOUTH);
			ui_upper.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_priority = new JLabel("优先级");
			ui_priority = new JTextField("");
			ui_priority.setBounds(100,100,100,50);
			ui_priority.setEditable(false);
			temppanel.add(ui_t_priority,BorderLayout.NORTH);
			temppanel.add(ui_priority,BorderLayout.SOUTH);
			ui_upper.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_timeusage = new JLabel("总时间");
			ui_timeusage = new JTextField("");
			ui_timeusage.setBounds(100,100,100,50);
			ui_timeusage.setEditable(false);
			temppanel.add(ui_t_timeusage,BorderLayout.NORTH);
			temppanel.add(ui_timeusage,BorderLayout.SOUTH);
			ui_upper.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_timeslice = new JLabel("时间片");
			ui_timeslice = new JTextField("");
			ui_timeslice.setBounds(100,100,100,50);
			ui_timeslice.setEditable(false);
			temppanel.add(ui_t_timeslice,BorderLayout.NORTH);
			temppanel.add(ui_timeslice,BorderLayout.SOUTH);
			ui_upper.add(temppanel);
			
			JPanel ui_lowerleft = new JPanel();
			ui_lowerleft.setLayout(new FlowLayout());
			ui_lowerleft.setBorder(BorderFactory.createTitledBorder("就绪队列"));
			ui_lower.add(ui_lowerleft);
			
			JPanel ui_lowerright = new JPanel();
			ui_lowerright.setLayout(new FlowLayout());
			ui_lowerright.setBorder(BorderFactory.createTitledBorder("阻塞队列"));
			ui_lower.add(ui_lowerright);
			
			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_r_processname = new JLabel("进程名称");
			ui_r_processname = new JList<String>();
			m_r_processname = create_listmodel();
			ui_r_processname.setModel(m_r_processname);
			temppanel.add(ui_t_r_processname,BorderLayout.NORTH);
			temppanel.add(ui_r_processname,BorderLayout.SOUTH);
			ui_lowerleft.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_r_priority = new JLabel("优先级");
			ui_r_priority = new JList<String>();
			m_r_priority = create_listmodel();
			ui_r_priority.setModel(m_r_priority);
			temppanel.add(ui_t_r_priority,BorderLayout.NORTH);
			temppanel.add(ui_r_priority,BorderLayout.SOUTH);
			ui_lowerleft.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_r_wait = new JLabel("等待时间");
			ui_r_wait = new JList<String>();
			m_r_wait = create_listmodel();
			ui_r_wait.setModel(m_r_wait);
			temppanel.add(ui_t_r_wait,BorderLayout.NORTH);
			temppanel.add(ui_r_wait,BorderLayout.SOUTH);
			ui_lowerleft.add(temppanel);
			

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_c_processname = new JLabel("进程名称");
			ui_c_processname = new JList<String>();
			m_c_processname = create_listmodel();
			ui_c_processname.setModel(m_c_processname);
			temppanel.add(ui_t_c_processname,BorderLayout.NORTH);
			temppanel.add(ui_c_processname,BorderLayout.SOUTH);
			ui_lowerright.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_c_priority = new JLabel("优先级");
			ui_c_priority = new JList<String>();
			m_c_priority = create_listmodel();
			ui_c_priority.setModel(m_c_priority);
			temppanel.add(ui_t_c_priority,BorderLayout.NORTH);
			temppanel.add(ui_c_priority,BorderLayout.SOUTH);
			ui_lowerright.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_c_wait = new JLabel("等待时间");
			ui_c_wait = new JList<String>();
			m_c_wait = create_listmodel();
			ui_c_wait.setModel(m_c_wait);
			temppanel.add(ui_t_c_wait,BorderLayout.NORTH);
			temppanel.add(ui_c_wait,BorderLayout.SOUTH);
			ui_lowerright.add(temppanel);

			temppanel = new JPanel();
			temppanel.setLayout(new BorderLayout());
			JLabel ui_t_c_reason = new JLabel("阻塞原因");
			ui_c_reason = new JList<String>();
			m_c_reason = create_listmodel();
			ui_c_reason.setModel(m_c_reason);
			temppanel.add(ui_t_c_reason,BorderLayout.NORTH);
			temppanel.add(ui_c_reason,BorderLayout.SOUTH);
			ui_lowerright.add(temppanel);
			
			window.validate();
		}
		cpu();
	}
	
	static DefaultListModel<String> create_listmodel()
	{
		DefaultListModel<String> create = new DefaultListModel<String>();
		for(int i = 0;i<10;i++)
		{
			create.addElement(" ");
		}
		return create;
	}
	
	public mainwindow()
	{
		return;
	}
	
	static void cpu()//模拟CPU
	{
		add_idleprocess();
		addtestprocess();
		addtestprocess();
		while(true)
		{
			if(PSW != 0)//中断处理
			{
				if(!INTP)
				{
					INTP = true;
				}
				INTR--;
				if(INTR == 0)
				{
					PSW = 0;
				}
			}
			else//无中断,读取指令并解析
			{
				pcbcurrent.status = 1;
				int ipbase = pcbcurrent.regprotection[2];//代码段基址
				IR = codesegment[ipbase+IP];//取指令
				int result = instruction();
				TIME++;
				if(result == -1)
				{//进程在本次执行之后结束,移除PCB释放资源,切换新进程
					processend();
					processswitch();
					continue;//切换至新进程后跳过时间片检查部分
				}
				else
				{
					IP++;//读取下一条指令
				}
			}
			Slice--;//分配的时间片-1
			if(Slice == 0)
			{//时间片耗尽,暂停进程,切换新进程
				processpause();
				processswitch();
			}
			CPUclock++;
			uiupd();
			//每次模拟一步之后暂停0.1秒钟
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
	}
	
	static int instruction()//指令解析,返回程序状态,0执行,1中断,-1结束
	{
		if(IR.matches("x=\\d"))
		{
			DR = Integer.parseInt(""+IR.charAt(2));
			return 0;
		}
		else if(IR.matches("!\\d\\d"))//中断
		{
			int interrupt = Integer.parseInt(""+IR.charAt(1));
			int intlength = Integer.parseInt(""+IR.charAt(2));
			pcbcurrent.status = 2;
			PSW = interrupt;
			INTR = intlength;
			INTP = false;
			return 1;
		}
		else if(IR == "x++")
		{
			DR++;
			return 0;
		}
		else if(IR == "x--")
		{
			DR--;
			return 0;
		}
		else if(IR == "end" )//进程结束,
		{
			return -1;
		}
		else if(IR == "gob")
		{
			IP = -1;//重置指针到代码段首
			return 0;
		}
		return 1;
	}
	
	static int processswitch()//进程切换,寻找下一项要执行的进程,并将其现场保护区恢复
	{
		int pcb_in_queue = processque.size();
		PCBindex = (PCBindex+1)%pcb_in_queue;
		pcbcurrent = processque.elementAt(PCBindex);//切换控制块
		pcbcurrent.status = 1;
		
		//恢复5项数据
		PID = pcbcurrent.PID;
		PSW = pcbcurrent.intrruptid;
		INTR = pcbcurrent.intrruptlength;
		IP = pcbcurrent.regprotection[0];
		DR = pcbcurrent.regprotection[1];
		TIME = pcbcurrent.time;
		
		//重置时间片
		Slice = 5;
		return 0;
	}
	
	static int processpause()//从当前进程换出,将现场保护区保存
	{
		if(pcbcurrent.status == 1)
		pcbcurrent.status = 0;
		
		//备份4项数据,PID对于每个进程是固定值无需修改
		pcbcurrent.intrruptid = PSW;
		pcbcurrent.intrruptlength = INTR;
		pcbcurrent.time = TIME;
		pcbcurrent.regprotection[0] = IP;
		pcbcurrent.regprotection[1] = DR;
		//记录最晚一次暂停的时间
		pcbcurrent.lastpause = CPUclock;
		return 0;
	}
	
	static int processend()//当前进程结束,输出结果,释放申请的全部内存(数据段和代码段),再释放PCB
	{
		//输出结果
		System.out.println("进程结束,结果为"+DR);
		//代码段的释放,并消除全局记录
		for(int i = code_usage.size();i > 0;)
		{
			i--;
			if(code_usage.elementAt(i)[2].intValue() == pcbcurrent.PID)
			{
				int base = code_usage.elementAt(i)[0].intValue();
				int length = code_usage.elementAt(i)[1].intValue();
				for(int j = 0;j<length;j++)
				{
					codesegment[base + j] = null;
				}
				code_usage.remove(i);
			}
		}
		//数据段的实际释放
		for(int i = 0;i < pcbcurrent.resources.size();i++)
		{
			//将申请到的空间全部释放并且把对应内存恢复为0
			Integer[] resource = pcbcurrent.resources.elementAt(i);
			int addrbase = resource[0];
			for(int j = 0;j < resource[1];j++)
			{
				datasegment[addrbase+j] = 0;
			}
		}
		//消除在全局的记录
		for(int i = data_usage.size();i > 0;)
		{
			i--;
			if(data_usage.elementAt(i)[2].intValue() == pcbcurrent.PID)
			{
				data_usage.remove(i);
			}
		}
		return 0;
	}
	
	static void resourcerec(int offset,int length)//内存资源回收(实际并未用到)
	{
		for(int i = 0;i < data_usage.size();i++)
		{
			if(data_usage.elementAt(i)[0].intValue() == offset&&data_usage.elementAt(i)[1].intValue() == length)
			{
				data_usage.remove(i);
			}
		}
	}
	
	static Integer[] resourcereq(int length,int PID)//内存资源申请,记录了申请者的PID
	{
		Integer[] resget = new Integer[2];
		resget[0] = new Integer(-1);
		resget[1] = new Integer(0);
		
		Integer[] reqsaving = new Integer[3];
		reqsaving[0] = new Integer(-1);
		reqsaving[1] = new Integer(0);
		reqsaving[2] = new Integer(PID);
		//遍历内存区域获取可用空间
		int resbase = 0;
		for(int i = 0;i < data_usage.size();i++)
		{
			if(data_usage.elementAt(i)[0]-resbase>=length)//足够长的空闲区段
			{
				resget[0] = new Integer(resbase);
				resget[1] = new Integer(length);
				reqsaving[0] = new Integer(resbase);
				reqsaving[1] = new Integer(length);
				data_usage.add(i,reqsaving);//插入原内存申请记录,仍保持从小到大的顺序
				return resget;
			}
			resbase = data_usage.elementAt(i)[0] + data_usage.elementAt(i)[1];//下一个空闲区段的首地址
		}
		if(512-resbase >= length)
		{
			resget[0] = new Integer(resbase);
			resget[1] = new Integer(length);
			reqsaving[0] = new Integer(resbase);
			reqsaving[1] = new Integer(length);
			data_usage.add(reqsaving);
		}
		return resget;
	}
	
	static void processcreate(String[] code)//创建进程,载入代码段,并根据代码段长度,申请一段内存空间
	{
		int length = code.length;
		int resbase = 0;
		PCB newpcb = null;
		Integer[] resget = new Integer[3];
		resget[0] = new Integer(-1);
		resget[1] = new Integer(0);
		resget[2] = new Integer(PCBIDcount+1);
		boolean memoryfound = false;
		for(int i = 0;i < code_usage.size();i++)
		{
			if(code_usage.elementAt(i)[0]-resbase>=length)//足够长的空闲区段
			{
				resget[0] = new Integer(resbase);
				resget[1] = new Integer(length);
				data_usage.add(i,resget);//插入原内存申请记录,仍保持从小到大的顺序
				memoryfound = true;
			}
			resbase = code_usage.elementAt(i)[0] + code_usage.elementAt(i)[1];//下一个空闲区段的首地址
		}
		if(!memoryfound && 512-resbase >= length)
		{
			resget[0] = new Integer(resbase);
			resget[1] = new Integer(length);
			code_usage.add(resget);
			memoryfound = true;
		}
		if(memoryfound)
		{
			newpcb = new PCB(PCBIDcount,resbase,length);
			PCBIDcount++;
			processque.add(newpcb);//添加进程到队列
			
			//载入代码
			for(int i = 0;i < length;i++)//将输入的代码复制到内存的代码段
			{
				codesegment[resbase+i] = code[i];
			}
		}
		if(pcbcurrent == null)
		{
			pcbcurrent = newpcb;
		}
	}
	
	static void add_idleprocess()//空闲进程
	{
		String[] code = new String[1];
		code[0] = "gob";
		processcreate(code);
	}
	
	static void uiupd()//可视化界面的刷新
	{
		ui_currentprocess.setText(""+PID);
		ui_instruction.setText(IR);
		ui_results.setText(""+DR);
		ui_priority.setText("0");
		ui_timeusage.setText(""+TIME);
		ui_timeslice.setText(""+Slice);
		
		m_r_processname.clear();
		m_r_priority.clear();
		m_r_wait.clear();
		m_c_processname.clear();
		m_c_priority.clear();
		m_c_wait.clear();
		m_c_reason.clear();
		
		
		for(int i = 0;i<processque.size();i++)
		{
			int index = (i + PCBindex)%processque.size();
			if(processque.elementAt(index).status == 0)
			{
				m_r_processname.addElement(""+processque.elementAt(index).PID);
				m_r_priority.addElement(""+processque.elementAt(index).PRIORITY);
				m_r_wait.addElement(""+(CPUclock-processque.elementAt(index).lastpause));
			}
			else if(processque.elementAt(index).status == 2)
			{
				m_c_processname.addElement(""+processque.elementAt(index).PID);
				m_c_priority.addElement(""+processque.elementAt(index).PRIORITY);
				m_c_wait.addElement(""+(CPUclock-processque.elementAt(index).lastpause));
				m_c_reason.addElement(""+processque.elementAt(index).intrruptid);
			}
		}
		
		
		addemptyitem(m_r_processname);
		addemptyitem(m_r_priority);
		addemptyitem(m_r_wait);
		addemptyitem(m_c_processname);
		addemptyitem(m_c_priority);
		addemptyitem(m_c_wait);
		addemptyitem(m_c_reason);
		
	}
	
	static DefaultListModel<String> addemptyitem(DefaultListModel<String> list)//UI,补充空出来的表格
	{
		for(int i = list.size();i<10;i++)
		{
			list.addElement(" ");
		}
		return list;
	}
	
	static void addtestprocess()//示例进程
	{
		String[] code = new String[]{
			"x=0","x++","x--","x=2","x=5","!39","gob"
		};
		processcreate(code);
	}
}
class PCB
{
	public final int PID;//进程id
	//public final int TYPE;//进程类型,空闲进程和用户进程
	public final int PRIORITY;//进程优先级
	public int status;//进程状态 0就绪1执行2阻塞
	public int intrruptid;//中断号
	public int intrruptlength;//中断时长
	public int time;
	public int lastpause;
	public int[] regprotection;//现场保护 [0]=IR,[1]=DR,[2]=IP基址,[3]=codelength
	public Vector<Integer[]> resources;//资源分配 每次分配资源产生一组[0]=数据段基址,[1]=数据段长度的数据
	
	PCB(int pcbid,/*int pcbtype,int pcbpriority,*/int codebase,int codelength)
	{
		this.PID = pcbid;
		//this.TYPE = pcbtype;
		this.PRIORITY = 0;
		this.status = 0;
		this.intrruptid = 0;
		this.intrruptlength = 0;
		this.time = 0;
		this.lastpause = mainwindow.CPUclock;
		this.regprotection = new int[4];
		this.regprotection[0] = 0;
		this.regprotection[1] = 0;
		this.regprotection[2] = codebase;
		this.regprotection[3] = codelength;
		this.resources = new Vector<Integer[]>();
	}
	
	public void finalize()//析构方法
	{
		this.resources.clear();
	}
}