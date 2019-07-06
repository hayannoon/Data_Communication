package stopwait;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer { // Application Layer

	public int nUpperLayerCount = 0; // upperLayer�� 0 �ʱ�ȭ
	public String pLayerName = null; // Layer�̸� ����
	public BaseLayer p_UnderLayer = null; // UnderLayer ����
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	// UpperLayer ����Ʈ ����
	public int count;
	byte[] receiveBuffer; // ���������� ���� �����ϰԵ� ����
	byte[][] buf; // Send��, ����ȭ�� �����͸� ������ ����
	int buflength; // ���� ���̸� ������ ����
	int bufcount; // receive�� �ݺ�Ƚ���� ������ ����
	int rotate; // �ݺ� Ƚ���� �����ϱ����� ����
	int sendCount; // send�� �ݺ�Ƚ���� ������ ����

	private class _CHAT_APP {

		byte[] capp_totlen; // �ѱ������� ���� (�̹� ���������� �Ⱦ���)
		byte capp_type; // Ÿ������ ����(�̹����������� �Ⱦ���)
		byte capp_unused;
		byte[] capp_data; // �����͸� ������ ����

		public _CHAT_APP() { // ������
			this.capp_totlen = new byte[2]; // 2����Ʈ(��������)
			this.capp_type = 0x00; // 1����Ʈ(Ÿ������)
			this.capp_unused = 0x00; // 1����Ʈ
			this.capp_data = null; // maximum 1456����Ʈ
		}
	} // ����� 4����Ʈ���� �� �� �ִ�.

	_CHAT_APP m_sHeader = new _CHAT_APP();
	// ��� ������ ���� ����

	public ChatAppLayer(String pName) { // ������
		// TODO Auto-generated constructor stub
		pLayerName = pName; // �Ű������� ���� String�� �̸����� ����
		ResetHeader();
	}

	public void ResetHeader() { // ����� �ʱ�ȭ�Ѵ�.
		for (int i = 0; i < 2; i++) {
			m_sHeader.capp_totlen[i] = (byte) 0x00;
		} // �ѱ��� 0���� ����
		m_sHeader.capp_data = null;// �����Ͱ� null����
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // ����� �����ش�.

		byte[] buf = new byte[length - 4]; // ����� 4����Ʈ�� ������ ũ�⸸ŭ�� �迭 ����

		for (int i = 0; i < length - 4; i++) // ó�� 4����Ʈ �����ϰ� �޺κи� ����.
			buf[i] = input[i + 4];

		return buf;
	}

	public synchronized boolean Receive(byte[] input) { // ���� �޾Ƶ��̴� �Լ�
		byte[] data; // ����Ʈ �迭 �������� ����
		if (input[2] == 0x00) {
			// ����ȭ�� ���� �����Ͷ��
			data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
			this.GetUpperLayer(0).Receive(data); // receive
			return true;
		}

		else { // ����ȭ�� �ִٸ�
			if (input[2] == 0x01) {
				// ù��° �����Ͷ��
				count = 0;
				int len = input[0] * 16 + input[1]; // ���� ���
				this.receiveBuffer = new byte[len]; // ���ۻ���
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
				System.arraycopy(data, 0, receiveBuffer, count, 10);// ���ۿ� ����
				count += 10; // ����index ����

			} // ù��° ���ۿ� ����
			else if (input[2] == 0x03) {
				// ������ ����ȭ �����̶��
				int len = input[0] * 16 + input[1]; // ���� ���
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�

				if (len % 10 == 0)
					System.arraycopy(data, 0, receiveBuffer, count, 10);
				else
					System.arraycopy(data, 0, receiveBuffer, count, len % 10); // ���ۿ� ����

				this.GetUpperLayer(0).Receive(receiveBuffer);
				// �����Ͱ� ��� ������ ������������ ���� ����!
			} else {
				// �߰�����ȭ �����̶��
				data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
				System.arraycopy(data, 0, receiveBuffer, count, 10); // ���ۿ� ����
				count += 10; // ���� index ����
			}
		}
		return true;
	}

	public boolean Send(byte[] input, int length) { // ����ٿ��� ������.

		m_sHeader.capp_totlen[1] = (byte) length;
		m_sHeader.capp_totlen[0] = (byte) (length >> 8);
		// �������� ����� ����
		buflength = length; // ���۱�������
		bufcount = 0;
		sendCount = 0;
		if (length % 10 == 0)
			rotate = length / 10;
		else
			rotate = (length / 10) + 1;
		// �ݺ� Ƚ�� ����
		buf = new byte[rotate][14]; // ����ȭ���� ��ü����
		if (length % 10 != 0)
			buf[rotate - 1] = new byte[length % 10 + 4];
		// ���������������� �̷��� ����
		for (int i = 0; i < rotate; i++) {
			if (i == rotate - 1) {
				// �����������ΰ��
				if (length % 10 == 0) {
					System.arraycopy(input, i * 10, buf[i], 4, 10);
				} else {
					System.arraycopy(input, i * 10, buf[i], 4, length % 10);
				}
			} else { // ������ �ƴ϶�� 10ĭ�� ����
				System.arraycopy(input, i * 10, buf[i], 4, 10);
			}
			buf[i][0] = m_sHeader.capp_totlen[0];
			buf[i][1] = m_sHeader.capp_totlen[1];
			buf[i][3] = m_sHeader.capp_unused;
		}
		
		// --------------------������� ����ȭ ��!--------------------------------

		if (buflength <= 10) {
			// 10���� ������ ����ȭ�����ʴ´�.
			m_sHeader.capp_type = 0x00; // ����ȭ���������� ǥ��
			buf[0][2] = m_sHeader.capp_type;
			this.GetUnderLayer().Send(buf[0], buflength + 4);
			// ������ �ٷ� �����ϰ�
			sendCount++;
			return true;
		} else
			this.Sendfragment();
		// ����ȭ�� �� �����Ͷ�� Sendfragmentȣ���ؼ� �ű⼭ �����ϵ��� �Ѵ�.
		return true;
	}

	public boolean Sendfragment() {
		// ����ȭ�� �����͸� ������� ���������� �Լ�

		if (sendCount == rotate) {
			// ���� ACK�� �޾Ҿ, �����������Ϳ����� ACK��� ���������ʾƾ��ϹǷ�, �������´�.
			return true;
		}
		if (rotate == 1)
			return true; // ����ȭ���µ������� ACK������ ���̻����������ʴ´�!
		// Ȥ�ø��� �ѹ��� ����ó���߽��ϴ�.

		else { // ����ȭ�� �ʿ��� ���

			if (sendCount == 0)
				m_sHeader.capp_type = 0x01; // ù��° ������ Ÿ���� 0x01
			else if (sendCount == rotate - 1)
				m_sHeader.capp_type = 0x03; // ������������ Ÿ���� 0x03;
			else
				m_sHeader.capp_type = 0x02; // �������� �߰��̹Ƿ� 0x02
			buf[sendCount][2] = m_sHeader.capp_type; // ���ۿ� Ÿ������ �Է�

			if (buf[sendCount][2] == 0x03) {
				// ������ ����ȭ �����̶��
				if (buflength % 10 == 0) { // �������� 10ĭ��ä����
					this.GetUnderLayer().Send(buf[sendCount], 14);
				} else { // �������� 10ĭ�� ��ä����� ���̸�ŭ�����ؼ�������.
					this.GetUnderLayer().Send(buf[sendCount], (buflength % 10) + 4);
				}
			} else {
				this.GetUnderLayer().Send(buf[sendCount], 14);
				// ó����,�߰��� ��� ���̰� 10+4�̹Ƿ� �̷��� ������.
			}

		}
		sendCount++;
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
