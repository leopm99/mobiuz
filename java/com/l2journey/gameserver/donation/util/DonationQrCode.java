package com.l2journey.gameserver.donation.util;
import java.util.Map;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.l2journey.gameserver.util.DDSConverter;
public final class DonationQrCode
{
 public static byte[] create(String data) throws WriterException
 {
  if (data == null || data.length() > 8192) throw new IllegalArgumentException("Invalid QR payload");
  return DDSConverter.convertToDDS(MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 128, 128, Map.of(EncodeHintType.MARGIN, 0)))).array();
 }
}
