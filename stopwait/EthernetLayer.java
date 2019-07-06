package stopwait;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {

	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	public _ETHERNET_Frame frame = new _ETHERNET_Frame();

	private class _ETHERNET_ADDR { // �̴��� �ּ� Ŭ����(6����Ʈ)
		private byte[] addr = new byte[6];

		public _ETHERNET_ADDR() {// ������(���δ� 0���� �ʱ�ȭ)
			for (int i = 0; i < 6; i++) {
				this.addr[i] = (byte) 0x00;
			}
		}
	}

	private class _ETHERNET_Frame {
		_ETHERNET_ADDR enet_dstaddr; // �̴��� ������ �ּ�
		_ETHERNET_ADDR enet_srcaddr; // �̴��� ����� �ּ�
		byte[] enet_type;
		byte[] enet_data;

		public _ETHERNET_Frame() { // ������
			this.enet_dstaddr = new _ETHERNET_ADDR(); // ������ �ּ� 6����Ʈ
			this.enet_srcaddr = new _ETHERNET_ADDR(); // ����� �ּ� 6����Ʈ
			this.enet_type = new byte[2]; // Ÿ�� ���� 2����Ʈ
			this.enet_data = null; // ������
		}// �̴��� ��� : 14 ����Ʈ
			// Ÿ�������� 0x01 : Data�� �����ٴ� �ǹ�
			// Ÿ�������� 0x02 : Ack�� �����ٴ� �ǹ�
	}

	_ETHERNET_Frame m_eHeader = new _ETHERNET_Frame();

	public void SetHeader(byte[] src, byte[] dst) {
		System.arraycopy(src, 0, m_eHeader.enet_srcaddr.addr, 0, src.length);
		System.arraycopy(dst, 0, m_eHeader.enet_dstaddr.addr, 0, dst.length);
	}

	public void ResetHeader() { // ����� �ʱ�ȭ�Ѵ�.
		for (int i = 0; i < 6; i++) {
			m_eHeader.enet_dstaddr.addr[i] = (byte) 0x00;
			m_eHeader.enet_srcaddr.addr[i] = (byte) 0x00;
		} // �ּҰ� 0���� �ʱ�ȭ
		m_eHeader.enet_data = null;// �����Ͱ� null����
	}

	public byte[] RemoveCappHeader(byte[] input, int length) { // ����� �����ش�.

		byte[] buf = new byte[length - 14]; // ����� 14����Ʈ�� ������ ũ�⸸ŭ�� �迭 ����
		for (int i = 0; i < length - 14; i++) // ó�� 14����Ʈ �����ϰ� �޺κи� ����.
			buf[i] = input[i + 14];
		return buf;
	}

	public boolean Send(byte[] input, int length) { // ����ٿ��� ������.
		byte[] buf = new byte[length + 14];

		System.arraycopy(m_eHeader.enet_dstaddr.addr, 0, buf, 0, 6);// ��6����Ʈ �����ּ� ����
		System.arraycopy(m_eHeader.enet_srcaddr.addr, 0, buf, 6, 6);// �׵ڿ� ����ּ� ����
		System.arraycopy(m_eHeader.enet_type, 0, buf, 12, 2);// Ÿ������ ����
		System.arraycopy(input, 0, buf, 14, length); // data�� ����
		buf[12] = 0x00;
		buf[13] = 1; // ���⼭ Send�� �����͸� �����°ŷ��Ѵ�.
		this.GetUnderLayer().Send(buf, length + 14);
		return true;

	}

	public boolean ackSend(byte[] input, int length) {
		byte[] buf = new byte[length + 14];

		System.arraycopy(m_eHeader.enet_dstaddr.addr, 0, buf, 0, 6);// ��6����Ʈ �����ּ� ����
		System.arraycopy(m_eHeader.enet_srcaddr.addr, 0, buf, 6, 6);// �׵ڿ� ����ּ� ����
		System.arraycopy(m_eHeader.enet_type, 0, buf, 12, 2);// Ÿ������ ����
		System.arraycopy(input, 0, buf, 14, length); // data�� ����
		buf[14] = 'A';
		buf[15] = 'C'; // ACK�� wireshark���� Ȯ���ϱ����� �ʿ�����ڸ��� ACK ���� ����
		buf[16] = 'K';
		buf[12] = 0x00;
		buf[13] = 0; // Ÿ�԰����� Ack��°� �˷��ش�.
		this.GetUnderLayer().Send(buf, length + 14); // �ٸ��� ACK������ ����
		return true;
	}

	public synchronized boolean Receive(byte[] input) { // ���� �޾Ƶ��̴� �Լ�
		byte[] data;

		if ((input[0] == 0xff) && (input[1] == 0xff) && (input[2] == 0xff) && (input[3] == 0xff) && (input[4] == 0xff)
				&& (input[5] == 0xff)) {
			// broadcasting�� ���������� ��Ŷ�� ���������� �޾��ش�.
			data = RemoveCappHeader(input, input.length);
			this.GetUpperLayer(0).Receive(data); // receive ����
			// �ּҼ���
			return true;
		}

		for (int i = 0; i < 6; i++) {
			if (input[i] != m_eHeader.enet_srcaddr.addr[i]) {
				return false; // ���� �ּҿ� ���� ��Ŷ�� �������� ��ġ���������� false
			}
			if (input[i + 6] != m_eHeader.enet_dstaddr.addr[i]) {
				return false; // ���� �������� ���� ��Ŷ�� ������� ��ġ���� ������ false
			}
		} // �ּҰ� �˻��ϰ� �ٸ��� false

		// ������� ����ߴٸ� �������� �ּҷ� ���°��̴�.
		if (input[13] == 0) {
			// ���� ���� input���� Ÿ���� Ack���
			((ChatAppLayer) this.GetUpperLayer(0)).Sendfragment(); // Sendfragment ȣ���Ѵ�.
			System.out.println("ACK"); // ������
		} else if (input[13] == 1) {
			// ���� input�� Data��� ��� ����, �¾����� �ø��� ACK������.
			data = RemoveCappHeader(input, input.length); // ��� �����ϰ�
			this.GetUpperLayer(0).Receive(data); // receive �����ϰ�
			this.ackSend(input, input.length); // Ack ������.
		}
		return true;
	}

	public EthernetLayer(String pName) {
		this.pLayerName = pName;
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
