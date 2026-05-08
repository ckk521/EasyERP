import { useState, useEffect } from 'react';
import { Modal, Form, Input, Select, InputNumber, Table, message, Space, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';

interface ExceptionRegisterProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  inboundOrder: {
    id: number;
    orderNo: string;
    supplierId: number;
    supplierCode: string;
    supplierName: string;
    warehouseId: number;
    warehouseCode: string;
    purchaseOrderId?: number;
    purchaseOrderNo?: string;
  };
  items: Array<{
    id: number;
    productId: number;
    skuCode: string;
    productName: string;
    barcode?: string;
    batchNo?: string;
    receivedQty?: number;
    inspectedQty?: number;
  }>;
  sourceType: 1 | 2; // 1-收货异常 2-验收异常
}

// 异常类型
const exceptionTypes = [
  { value: 1, label: '破损' },
  { value: 2, label: '短缺' },
  { value: 3, label: '质量不合格' },
  { value: 4, label: '错货' },
  { value: 5, label: '其他' },
];

interface SelectedItem {
  key: string;
  productId: number;
  skuCode: string;
  productName: string;
  barcode?: string;
  batchNo?: string;
  maxQty: number;
  exceptionQty: number;
  inboundItemId: number;
}

export default function ExceptionRegister({
  visible,
  onCancel,
  onSuccess,
  inboundOrder,
  items,
  sourceType,
}: ExceptionRegisterProps) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [selectedItems, setSelectedItems] = useState<SelectedItem[]>([]);
  const [zones, setZones] = useState<Array<{ id: number; code: string; name: string }>>([]);
  const [locations, setLocations] = useState<Array<{ id: number; code: string }>>([]);

  useEffect(() => {
    if (visible) {
      loadZones();
      form.resetFields();
      setSelectedItems([]);
    }
  }, [visible]);

  const loadZones = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/api/v1/base/zones?warehouseId=${inboundOrder.warehouseId}&type=3&limit=100`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setZones(result.data.list || []);
      }
    } catch (error) {
      console.error('加载库区失败', error);
    }
  };

  const loadLocations = async (zoneId: number) => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/api/v1/base/locations?zoneId=${zoneId}&status=1&limit=100`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const result = await res.json();
      if (result.success) {
        setLocations(result.data.list || []);
      }
    } catch (error) {
      console.error('加载库位失败', error);
    }
  };

  const handleAddItem = (item: typeof items[0]) => {
    const exists = selectedItems.find((i) => i.productId === item.productId);
    if (exists) {
      message.warning('该商品已添加');
      return;
    }
    setSelectedItems([
      ...selectedItems,
      {
        key: String(item.productId),
        productId: item.productId,
        skuCode: item.skuCode,
        productName: item.productName,
        barcode: item.barcode,
        batchNo: item.batchNo,
        maxQty: sourceType === 1 ? (item.receivedQty || 0) : (item.inspectedQty || 0),
        exceptionQty: 1,
        inboundItemId: item.id,
      },
    ]);
  };

  const handleUpdateQty = (productId: number, qty: number) => {
    setSelectedItems(
      selectedItems.map((item) =>
        item.productId === productId ? { ...item, exceptionQty: qty } : item
      )
    );
  };

  const handleRemoveItem = (productId: number) => {
    setSelectedItems(selectedItems.filter((item) => item.productId !== productId));
  };

  const handleSubmit = async () => {
    if (selectedItems.length === 0) {
      message.error('请选择异常商品');
      return;
    }

    const values = await form.validateFields();
    setLoading(true);

    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/v1/exception-orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          inboundOrderId: inboundOrder.id,
          inboundOrderNo: inboundOrder.orderNo,
          purchaseOrderId: inboundOrder.purchaseOrderId,
          purchaseOrderNo: inboundOrder.purchaseOrderNo,
          supplierId: inboundOrder.supplierId,
          supplierCode: inboundOrder.supplierCode,
          supplierName: inboundOrder.supplierName,
          warehouseId: inboundOrder.warehouseId,
          warehouseCode: inboundOrder.warehouseCode,
          zoneId: values.zoneId,
          zoneCode: zones.find((z) => z.id === values.zoneId)?.code,
          exceptionType: values.exceptionType,
          exceptionReason: values.exceptionReason,
          sourceType,
          remark: values.remark,
          items: selectedItems.map((item) => ({
            productId: item.productId,
            skuCode: item.skuCode,
            productName: item.productName,
            barcode: item.barcode,
            batchNo: item.batchNo,
            exceptionQty: item.exceptionQty,
            exceptionType: values.exceptionType,
            inboundItemId: item.inboundItemId,
          })),
        }),
      });

      const result = await res.json();
      if (result.success) {
        message.success('异常登记成功');
        onSuccess();
      } else {
        message.error(result.message || '登记失败');
      }
    } catch (error) {
      message.error('登记失败');
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<typeof items[0]> = [
    { title: 'SKU编码', dataIndex: 'skuCode', width: 100 },
    { title: '商品名称', dataIndex: 'productName', width: 150 },
    { title: '条码', dataIndex: 'barcode', width: 100 },
    {
      title: sourceType === 1 ? '收货数量' : '验收数量',
      dataIndex: sourceType === 1 ? 'receivedQty' : 'inspectedQty',
      width: 80,
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => (
        <Button size="small" type="link" onClick={() => handleAddItem(record)}>
          添加
        </Button>
      ),
    },
  ];

  const selectedColumns: ColumnsType<SelectedItem> = [
    { title: 'SKU编码', dataIndex: 'skuCode', width: 100 },
    { title: '商品名称', dataIndex: 'productName', width: 150 },
    {
      title: '异常数量',
      dataIndex: 'exceptionQty',
      width: 120,
      render: (qty: number, record) => (
        <InputNumber
          min={1}
          max={record.maxQty}
          value={qty}
          onChange={(v) => handleUpdateQty(record.productId, v || 1)}
          size="small"
        />
      ),
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => (
        <Button size="small" type="link" danger onClick={() => handleRemoveItem(record.productId)}>
          移除
        </Button>
      ),
    },
  ];

  return (
    <Modal
      title="异常登记"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={800}
      okText="提交"
    >
      <Form form={form} layout="vertical">
        <Form.Item label="入库单号">
          <Input value={inboundOrder.orderNo} disabled />
        </Form.Item>
        <Form.Item label="供应商">
          <Input value={inboundOrder.supplierName} disabled />
        </Form.Item>
        <Form.Item label="隔离库区" name="zoneId" rules={[{ required: true, message: '请选择隔离库区' }]}>
          <Select
            placeholder="选择隔离库区"
            onChange={(v) => loadLocations(v)}
            options={zones.map((z) => ({ value: z.id, label: `${z.code} - ${z.name}` }))}
          />
        </Form.Item>
        <Form.Item label="异常类型" name="exceptionType" rules={[{ required: true, message: '请选择异常类型' }]}>
          <Select placeholder="选择异常类型" options={exceptionTypes} />
        </Form.Item>
        <Form.Item label="异常原因" name="exceptionReason">
          <Input.TextArea rows={2} placeholder="请输入异常原因" maxLength={500} />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={2} placeholder="请输入备注" maxLength={500} />
        </Form.Item>
      </Form>

      <div style={{ marginBottom: 8 }}>
        <strong>可选商品（点击添加）</strong>
      </div>
      <Table
        columns={columns}
        dataSource={items}
        rowKey="id"
        size="small"
        pagination={false}
        scroll={{ y: 150 }}
        style={{ marginBottom: 16 }}
      />

      <div style={{ marginBottom: 8 }}>
        <strong>已选异常商品</strong>
      </div>
      <Table
        columns={selectedColumns}
        dataSource={selectedItems}
        rowKey="key"
        size="small"
        pagination={false}
        scroll={{ y: 150 }}
      />
    </Modal>
  );
}
