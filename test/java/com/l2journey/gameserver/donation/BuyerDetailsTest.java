package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class BuyerDetailsTest
{
 @Test void allFieldsMayBeSkippedOrBlank() {
  assertEquals(BuyerDetails.EMPTY,BuyerDetails.parseForm("skip"));
  assertEquals(BuyerDetails.EMPTY,BuyerDetails.parseForm("set||||"));
  assertEquals(BuyerDetails.EMPTY,BuyerDetails.parseForm("set|0|0|0|"));
 }
 @Test void preservesCompoundUnicodeNamesAndEmptySurname() {
  var details=BuyerDetails.parseForm("set|  João  Pedro |D’Ávila||");
  assertEquals("João Pedro",details.firstName()); assertEquals("D’Ávila",details.lastName()); assertNull(details.cpf());
  assertNull(BuyerDetails.parseForm("set|Ana|||").lastName());
  assertEquals(new BuyerDetails("Ana Maria","Silva","52998224725"),BuyerDetails.parseForm("set| Ana Maria | Silva | 52998224725 |"));
 }
 @Test void rejectsMarkupCommandsAndControlCharacters() {
  for(String first : new String[] {"<img>", "Ana|Maria", "Ana\nMaria", "Ana\tMaria", "A\u202eB", "Test1"})
   assertThrows(IllegalArgumentException.class,() -> new BuyerDetails(first,null,null));
 }
 @Test void rejectsMalformedFieldBoundaries() {
  for(String command : new String[] {"set|Ana|Silva", "set|Ana|Silva||extra", "set|Ana|Silva|0|extra|", "other"})
   assertThrows(IllegalArgumentException.class,() -> BuyerDetails.parseForm(command));
 }
 @Test void validatesCpfCheckDigitsAndNormalizesPunctuation() {
  assertEquals("52998224725",new BuyerDetails(null,null,"529.982.247-25").cpf());
  assertEquals("52998224725",new BuyerDetails(null,null,"52998224725").cpf());
  for(String cpf : new String[] {"52998224724", "11111111111", "00000000000", "123", "529abc98224725"})
   assertThrows(IllegalArgumentException.class,() -> new BuyerDetails(null,null,cpf));
 }
 @Test void limitsNameLengthAndNormalizesComposedCharacters() {
  assertThrows(IllegalArgumentException.class,() -> new BuyerDetails("A".repeat(65),null,null));
  assertEquals("José",new BuyerDetails("Jose\u0301",null,null).firstName());
 }
 @Test void personalDataIsNotExposedByToStringOrValidationErrors() {
  var details=new BuyerDetails("Ana", "Silva", "52998224725");
  assertFalse(details.toString().contains("52998224725")); assertFalse(details.toString().contains("Ana"));
  var failure=assertThrows(IllegalArgumentException.class,() -> new BuyerDetails(null,null,"52998224724"));
  assertFalse(failure.getMessage().contains("52998224724"));
 }
}
