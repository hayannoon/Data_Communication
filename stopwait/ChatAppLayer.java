package stopwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {	//Application Layer

	public int nUpperLayerCount = 0;	//upperLayer수 0 초기화
	public String pLayerName = null;	//Layer이름 변수
	public BaseLayer p_UnderLayer = null; //UnderLayer 변수
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	//UpperLayer 리스트 생성
	
	private class _CHAT_APP {
				
	byte[] capp_totlen ; //총길이정보 저장 (이번 과제에서는 안쓰임)
	byte capp_type;  	//타입정보 저장(이번과제에서는 안쓰임)
	byte capp_unused;  
	byte[] capp_data;	//데이터를 저장할 변수
	
	public _CHAT_APP () {	//생성자
		this.capp_totlen = new byte[2];		//2바이트(길이정보)
		this.capp_type = 0x00;				//1바이트(타입정보)
		this.capp_unused = 0x00;			//1바이트
		this.capp_data = null;				//maximum 1456바이트
		}
	}	//헤더는 4바이트임을 알 수 있다.
	
	
	_CHAT_APP m_sHeader = new _CHAT_APP();
	//헤더 저장할 변수 생성
	
	public ChatAppLayer(String pName) {		//생성자
		// TODO Auto-generated constructor stub
		pLayerName = pName;		//매개변수로 들어온 String을 이름으로 설정
		ResetHeader();
	}
	
	public void ResetHeader() {		//헤더를 초기화한다.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}	//총길이 0으로 설정
		m_sHeader.capp_data = null;//데이터값 null설정
	}
	

	public byte[] RemoveCappHeader(byte[] input, int length) {	//헤더를 없애준다.
		
		byte[] buf = new byte[length-4];	//헤더의 4바이트를 제외한 크기만큼의 배열 생성
		
		for(int i = 0 ; i < length-4 ; i++)	//처음 4바이트 제외하고 뒷부분만 복사.
			buf[i] = input[i+4];
		
		return buf;
	}
	
	public synchronized boolean Receive(byte[] input) {	//정보 받아들이는 함수
		byte[] data;	//바이트 배열 참조변수 선언
		
		data = RemoveCappHeader(input, input.length);	//헤더 제거하고
		this.GetUpperLayer(0).Receive(data);		//receive 
		// 주소설정
		return true;
	}
	
	public boolean Send(byte[] input, int length) {		//헤더붙여서 보낸다.
		byte[] buf = new byte[length+4];
		buf[0] = m_sHeader.capp_totlen[0];
		buf[1] = m_sHeader.capp_totlen[1];
		buf[2] = m_sHeader.capp_type;
		buf[3] = m_sHeader.capp_unused;
		
		for(int i = 0 ; i < length ; i++) {
			buf[i+4] = input[i];
		}
		
		this.GetUnderLayer().Send(buf, length + 4);
		return true;
	}
	

	@Override
	public String GetLayerName() {
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		// TODO Auto-generated method stub
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}
	
}
