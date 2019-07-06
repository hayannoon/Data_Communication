package chat_file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import chat_file.ChatAppLayer.Receive_Thread;

public class FileAppLayer implements BaseLayer {

	public int nUpperLayerCount = 0; // upperLayer수 0 초기화
	public String pLayerName = null; // Layer이름 변수
	public BaseLayer p_UnderLayer = null; // UnderLayer 변수
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();

	public class _FAPP_HEADER { // fileapp header 클래스
		byte[] fapp_totlen; // 전체길이
		byte[] fapp_type; // 타입정보
		byte fapp_msg_type; // 메시지타입
		byte ed;
		byte[] fapp_seq_num; // sequence number
		byte[] fapp_data; // file data

		public _FAPP_HEADER() { // 헤더 생성자
			this.fapp_totlen = new byte[4]; // 전체길이 4바이트
			this.fapp_type = new byte[2]; // 타입정보 2바이트(단편화 정보를 담는다.)
			this.fapp_msg_type = 0x00; // 사용안함
			this.ed = 0x00; // 사용안함
			this.fapp_seq_num = new byte[4]; // 시퀀스넘버 4바이트 파일을 몇%보냈는지 알기위해 사용(progressbar)
			this.fapp_data = null; // 파일데이터 최대 1448바이트
		} // 헤더는 총 12바이트
	}

	private void makeHeader(int row) { //헤더 생성하는 함수
		byte[] header = new byte[12];
		System.arraycopy(m_sHeader.fapp_totlen, 0, header, 0, 4);// 길이
		System.arraycopy(m_sHeader.fapp_type, 0, header, 4, 2);// fapp타입
		header[6] = m_sHeader.fapp_msg_type;

		System.arraycopy(m_sHeader.fapp_seq_num, 0, header, 8, 4);
		System.arraycopy(header, 0, send_Buffer[row], 0, 12);
	} //row를 인자로 넣으면 해당 행에 헤더정보가 복사된다.

	_FAPP_HEADER m_sHeader = new _FAPP_HEADER(); // 헤더 생성
	byte[] file_To_Bytes;
	byte[][] send_Buffer;
	byte[] receive_Buffer;
	String receiveFileName;
	int receive_Rotation = 0;
	int receive_Length = 0;

	public FileAppLayer(String pName) { // 생성자
		// TODO Auto-generated constructor stub
		pLayerName = pName; // 매개변수로 들어온 String을 이름으로 설정
		// ResetHeader();
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // 헤더를 없애준다.

		byte[] buf = new byte[length - 12]; // 헤더의 12바이트를 제외한 크기만큼의 배열 생성

		for (int i = 0; i < length - 12; i++) // 처음 12바이트 제외하고 뒷부분만 복사.
			buf[i] = input[i + 12];

		return buf;
	}

	public class Send_Thread implements Runnable {
		File file;

		public Send_Thread(File file) {
			this.file = file; // 전송할 파일을 매개변수로 받아온다.
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			realSend(file);
		}
	}
	
	public boolean Send(String filePath) {

		File file = new File(filePath); // 전송할 파일 저장
		Send_Thread sendThread = new Send_Thread(file); // 쓰레드생성
		Thread obj = new Thread(sendThread);
		obj.start();
		return false;
	}

	@SuppressWarnings("resource")
	public byte[] getBytesFromFile(File file) throws IOException { // 파일을 바이트로 바꾸는 함수
		InputStream is = new FileInputStream(file);
		long length = file.length();

		byte[] bytes = new byte[(int) length]; //파일-> 바이트 값을 저장할 바이트 배열 생성

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file" + file.getName());
		}

		is.close();
		return bytes; //만들어진 바이트배열 반환
	}

	public boolean realSend(File file) {

		long length; // 파일 길이
		int rotation = 0; // 반복횟수(1448바이트짜리로 몇번잘라야하는가)

		// length = file.length(); // 파일 길이를 바이트 단위로 반환
		length = file.length();
		if (length % 1448 == 0)
			rotation = (((int) length) / 1448) + 1; // 첫부분,마지막부분까지
		else
			rotation = ((int) length / 1448) + 2; //// 첫부분,마지막부분까지 반복횟수 저장
		try { // 파일을 바이트로 바꾼값 저장
			file_To_Bytes = getBytesFromFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		send_Buffer = new byte[rotation][1460];
		send_Buffer[0] = new byte[12 + file.getName().getBytes().length]; // 첫부분은 헤더,파일이름만 보낸다.
		// send_Buffer[rotation-1] = new byte[12 + ((int)length/1448)+1];
		send_Buffer[rotation - 1] = new byte[12 + ((int) length % 1448)];
		// 마지막부분은 이렇게 설정

		// 헤더를 체울 차례
		byte[] len = intToByte4((int) length);
		m_sHeader.fapp_totlen = len; // 길이정보 저장

		for (int i = 0; i < rotation; i++) {
			if (i == 0) {
				m_sHeader.fapp_type = intToByte2(0);
				// 첫부분은 타입 0x00
				m_sHeader.fapp_msg_type = 0x00;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber 저장
				makeHeader(i);
				// 헤더 저장
				byte[] tempName = file.getName().getBytes(); // 파일이름바이트로저장
				System.arraycopy(tempName, 0, send_Buffer[i], 12, tempName.length);
			}

			else if (i == rotation - 1) {
				m_sHeader.fapp_type = intToByte2(2);
				// 마지막부분은 타입 0x02
				m_sHeader.fapp_msg_type = 0x01;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber 저장
				makeHeader(i);
				System.arraycopy(file_To_Bytes, (i - 1) * 1448, send_Buffer[i], 12, (int) length % 1448);

			} else {
				m_sHeader.fapp_type = intToByte2(1);// 중간부분은 타입0x01
				m_sHeader.fapp_msg_type = 0x01;
				m_sHeader.fapp_seq_num = intToByte4(i - 1);
				// sequencenumber 저장
				makeHeader(i);
				// 헤더 잘 저장한뒤
				System.arraycopy(file_To_Bytes, (i - 1) * 1448, send_Buffer[i], 12, 1448);
				// 파일들 저장
			}
		}
		// ------------------------여기까지 파일 단편화 끝!--------------------------
		for (int i = 0; i < rotation; i++) {
			this.GetUnderLayer().Send(send_Buffer[i], send_Buffer[i].length);
			int forProgressBar = (int) ((((double) i) / (rotation - 1)) * 100);// 보내면서 프로그레스바 업데이트
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);
			
			//**********************프로그램의 정확성/속도는 여기서 결정된다.*************************
			if ((i % 5 == 0)) {	
				try {
					Thread.sleep(1); //보내고 잠시 대기
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//**********************프로그램의 정확성/속도는 여기서 결정된다.*************************
		}
		return true;
	}

	

	public class Receive_Thread implements Runnable {

		byte[] data;

		public Receive_Thread(byte[] data) { // 생성자
			this.data = data;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Realreceive(data);//realReceive 호출한다.
		}

	}

	public synchronized boolean Receive(byte[] input) { //하위에서 호출하는 Receive함수
		Receive_Thread receiveThread = new Receive_Thread(input);
		Thread obj = new Thread(receiveThread);//쓰레드생성
		obj.start(); //run을 실행한다.
		return false;

	}

	public synchronized boolean Realreceive(byte[] input) {// 실질적으로 Receive가 구현되는곳
		if ((input[4] == 0x00) && (input[6] == 0x00)) { // 첫번째 조각인경우-파일이름을 담고있다.
			byte[] tempForLength = new byte[4]; // 길이를 구하려고 임시로 만든 바이트배열 변수
			System.arraycopy(input, 0, tempForLength, 0, 4);// 길이정보 복사
			receive_Length = byteToInt(tempForLength); // 전체길이 저장
			if (receive_Length % 1448 == 0)
				receive_Rotation = ((receive_Length) / 1448); // 첫부분,마지막부분까지
			else
				receive_Rotation = (receive_Length / 1448) + 1; //// 첫부분,마지막부분까지 반복횟수 저장

			byte[] tempForName = RemoveCappHeader(input, input.length); // 헤더 저장
			receiveFileName = new String(tempForName); // 파일이름 저장
			receive_Buffer = new byte[receive_Length];
		} else if ((input[4] == 0x01) && (input[6] == 0x01)) { // 데이터이고, 마지막조각이 아닌경우
			byte[] tempForSequenceNumber = new byte[4];
			System.arraycopy(input, 8, tempForSequenceNumber, 0, 4); // sequence Number 저장
			int sNumber = byteToInt(tempForSequenceNumber); // sequenceNubmer저장
			byte[] data = RemoveCappHeader(input, input.length); // 헤더 제거
			System.arraycopy(data, 0, receive_Buffer, 1448 * sNumber, 1448); // sequenceNumber에 맞게 데이터 저장
			int forProgressBar = (int) ((((double) sNumber) / (receive_Rotation - 1)) * 100);
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);// progressbar 수정

		} else { // 타입이 0x02인경우 -> 마지막 데이터가 들어온 경우
			byte[] tempForSequenceNumber = new byte[4];
			System.arraycopy(input, 8, tempForSequenceNumber, 0, 4); // sequence Number 저장
			int sNumber = byteToInt(tempForSequenceNumber); // sequenceNubmer저장
			byte[] data = RemoveCappHeader(input, input.length); // 헤더 제거
			
			
			System.arraycopy(data, 0, receive_Buffer, 1448 * sNumber, data.length); // sequenceNumber에 맞게 데이터 저장
			int forProgressBar = (int) ((((double) sNumber) / (receive_Rotation - 1)) * 100);
			((ChatFileDlg) p_aUpperLayer.get(0)).progressBar.setValue(forProgressBar);// progressbar 수정
			// 이때는 receive_Buffer가 가득 차있다. 이제 파일로 만들어서 올리기만하면 된다.
			
			try { //혹시 마지막 프레임은 왔지만 그 앞에것들이 안왔을경우를 대비해서 기다려준다.
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			((ChatFileDlg) this.GetUpperLayer(0)).ReceiveFile(receiveFileName, receive_Buffer);
		}

		return false;
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

	byte[] intToByte2(int value) { // int형 (10진수) 받아서 바이트형식으로 변환(2바이트)
		byte[] temp = new byte[2];
		temp[1] = (byte) (value >> 8);
		temp[0] = (byte) value;

		return temp;
	}

	byte[] intToByte4(int value) {// int형 (10진수) 받아서 바이트형식으로 변환(4바이트)
		byte[] temp = new byte[4];

		temp[0] |= (byte) ((value & 0xFF000000) >> 24);
		temp[1] |= (byte) ((value & 0xFF0000) >> 16);
		temp[2] |= (byte) ((value & 0xFF00) >> 8);
		temp[3] |= (byte) (value & 0xFF);

		return temp;
	}

	public int byteToInt(byte[] arr) { // byte를 int로 바꾸는 함수
		return (arr[0] & 0xff) << 24 | (arr[1] & 0xff) << 16 | (arr[2] & 0xff) << 8 | (arr[3] & 0xff);
	}

	public File makeFileFromBytes(String fileName, byte[] data) { // 데이터를 파일로 만들어준다.
		File file = new File(fileName);
		try {
			OutputStream os = new FileOutputStream(file);
			os.write(data);
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	}

}
