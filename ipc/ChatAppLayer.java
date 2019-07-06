package ipc;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	private class _CAPP_HEADER {
		int capp_src;
		int capp_dst;
		byte[] capp_totlen;
		byte[] capp_data;

		public _CAPP_HEADER() {
			this.capp_src = 0x00000000;
			this.capp_dst = 0x00000000;
			this.capp_totlen = new byte[2];
			this.capp_data = null;
		}
	}

	_CAPP_HEADER m_sHeader = new _CAPP_HEADER();

	public ChatAppLayer(String pName) {		//생성자
		// TODO Auto-generated constructor stub
		pLayerName = pName;		//매개변수로 들어온 String을 이름으로 설정
		ResetHeader();
	}

	public void ResetHeader() {		//헤더를 초기화한다.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		}
		m_sHeader.capp_data = null;
	}

	public byte[] ObjToByte(_CAPP_HEADER Header, byte[] input, int length) {//input에 header를 추가한다.
		byte[] buf = new byte[length + 10];			
		byte[] srctemp = intToByte4(Header.capp_src);	//헤더의 근원지 가져오고
		byte[] dsttemp = intToByte4(Header.capp_dst);	//헤더의 도착지 가져오고

		buf[0] = dsttemp[0];	
		buf[1] = dsttemp[1];
		buf[2] = dsttemp[2];
		buf[3] = dsttemp[3];
		buf[4] = srctemp[0];
		buf[5] = srctemp[1];
		buf[6] = srctemp[2];
		buf[7] = srctemp[3];	//헤더의 주소들을 사용해서 초기화해준다.
		buf[8] = (byte) (length % 256);		//길이의 바이트 첫째자리계산 및 저장
		buf[9] = (byte) (length / 256);		//길이의 바이트 둘째자리 계산 및 저장

		for (int i = 0; i < length; i++)	
			buf[10 + i] = input[i];		//for문돌면서 헤더이후 문자가 저장된 바이트 저장

		return buf;		//만들어 바이트배열 반환
	}

	public boolean Send(byte[] input, int length) {		//input받아서 보내준다.
		
		byte[] bytes = ObjToByte(m_sHeader, input, length);
		this.GetUnderLayer().Send(bytes, length + 10);
		
		return true;
	}

	public byte[] RemoveCappHeader(byte[] input, int length) {	//헤더를 없애준다.
		
		byte[] buf = new byte[length-10];	//헤더의 10바이트를 제외한 크기만큼의 배열 생성
		
		for(int i = 0 ; i < length-10 ; i++)	//처음 10바이트 제외하고 뒷부분만 복사.
			buf[i] = input[i+10];
		
		return buf;
	}
	
	public synchronized boolean Receive(byte[] input) {	//정보 받아들이는 함수
		byte[] data;
		byte[] temp_src = intToByte4(m_sHeader.capp_src);
		
		for (int i = 0; i < 4; i++) {
			if (input[i] != temp_src[i]) {
				return false;
			}		//주소값 검사하고, 다르면 false
		}
		
		data = RemoveCappHeader(input, input.length);	//헤더 제거하고
		this.GetUpperLayer(0).Receive(data);		//receive 실행
		// 주소설정
		return true;
	}

	byte[] intToByte2(int value) {			//int형 (10진수) 받아서 바이트형식으로 변환(2바이트)
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {//int형 (10진수) 받아서 바이트형식으로 변환(4바이트)
		byte[] temp = new byte[4];
		
		temp[0] |= (byte) ((value & 0xFF000000) >> 24 );
		temp[1] |= (byte) ((value & 0xFF0000) >> 16 );
		temp[2] |= (byte) ((value & 0xFF00) >> 8 );
		temp[3] |= (byte) (value & 0xFF);
		
		return temp;
	}
	@Override
	public String GetLayerName() {	//레이어이름 반환
		// TODO Auto-generated method stub
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {	//하위 레이어 반환
		// TODO Auto-generated method stub
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {	//상위 레이어 반환
		// TODO Auto-generated method stub
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {	//하위 레이어 세팅
		// TODO Auto-generated method stub
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {	//상위 레이어 세팅
		// TODO Auto-generated method stub
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;

	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {	//상위 레이어 세팅하고, 그 레이어의 하위를 함수 호출 객체로 설정
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);
	}

	public void SetEnetSrcAddress(int srcAddress) {	//근원지 주소 받아서  헤더로 전달
		// TODO Auto-generated method stub
		m_sHeader.capp_src = srcAddress;
	}

	public void SetEnetDstAddress(int dstAddress) {	//도착지 주소 받아서 헤더로 전달
		// TODO Auto-generated method stub
		m_sHeader.capp_dst = dstAddress;
	}

}
