package com.assignment.schlewinow.internetofthings;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Parcelable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Pops up when a NFC-tag was scanned.
 * Reads and output available data.
 */
public class MainActivity extends Activity
{
    /**
     * Shows main data read from tag.
     */
    private TextView mMainOutput = null;

    /**
     * Shows "raw" data read from tag.
     */
    private TextView mDataOutput = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mMainOutput = (TextView)this.findViewById(R.id.mainOutput);
        this.mDataOutput = (TextView)this.findViewById(R.id.rawOutput);
        this.mDataOutput.setVisibility(View.GONE);

        // Allows to show/hide the content of the tag.
        Button showButton = (Button)this.findViewById(R.id.showRawContent);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Button button = (Button)view;
                if (mDataOutput.isShown())
                {
                    mDataOutput.setVisibility(View.GONE);
                    button.setText("Show raw content");
                }
                else
                {
                    mDataOutput.setVisibility(View.VISIBLE);
                    button.setText("Hide raw content");
                }
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Code from docs: https://developer.android.com/guide/topics/connectivity/nfc/nfc.html
        Intent intent = this.getIntent();
        NdefMessage[] messages = null;

        if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                messages = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            }
        }

        // Read ID and supported technologies from the tag.
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tagFromIntent != null)
        {
            // Collect data.
            String tagID = this.readID(tagFromIntent.getId(), " ");
            String tagTechs = this.readSupportedTechs(tagFromIntent, "  -", "\n");

            // Output data to user.
            String tagOutput = ">> TAG <<\n";
            tagOutput += "ID: " + tagID + "\n";
            tagOutput += "Supported Techs:\n" + tagTechs + "\n";

            this.mMainOutput.setText(tagOutput);
        }

        // process NDEF-messages
        if(messages != null)
        {
            for (NdefMessage message : messages)
            {
                NdefRecord header = message.getRecords()[0];

                //System.out.println("describe: " + header.describeContents());
                //message.getByteArrayLength();

                // Collect data.
                String typeNameFormat = this.readTypeNameFormat(header.getTnf());
                String id = this.readID(header.getId(), " ");
                String recordType = readRecordType(header.getType(), header.getTnf());

                String dataAsText1 = readDataAsString(message.getRecords(), 1);
                String dataAsText2 = readDataAsString(message.getRecords(), 2);
                String dataAsHex = readDataAsHex(message.getRecords());
                String dataAsBytes = readDataAsByte(message.getRecords());

                // Output data to user.
                String infoOutput = this.mMainOutput.getText() + "\n>> MESSAGE <<\n";
                infoOutput += "TNF: " + typeNameFormat + "\n";
                infoOutput += "ID: " + id + "\n";
                infoOutput += "Record Type: " + recordType + "\n";
                infoOutput += "MIME Type: " + header.toMimeType() + "\n";
                infoOutput += "URI: " + header.toUri() + "\n\n";

                String dataOutput = "\n>> PAYLOAD <<\n";
                dataOutput += "Single Byte Char (ASCII):\n" + dataAsText1 + "\n\n";
                dataOutput += "Two Byte Char (UTF16):\n" + dataAsText2 + "\n\n";
                dataOutput += "Hexadecimal:\n" + dataAsHex + "\n\n";
                dataOutput += "Decimal:\n" + dataAsBytes + "\n";

                this.mMainOutput.setText(infoOutput);
                this.mDataOutput.setText(dataOutput);
            }
        }
        else if(intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED))
        {
            String payload = this.readMifareTag(tagFromIntent);

            String dataOutput = "\n>> MIFARE PAYLOAD <<\n";
            dataOutput += payload + "\n";
            this.mDataOutput.setText(dataOutput);
        }
    }

    /**
     * Read tag tech list into string.
     * @param tag The tag to analyze.
     * @param pre String put in front of every tech.
     * @param separator String to put between techs.
     * @return String containing supported techs of given tag.
     */
    private String readSupportedTechs(Tag tag, String pre, String separator)
    {
        String tagTechs = "";
        if (tag != null)
        {
            for (int index = 0; index < tag.getTechList().length; ++index)
            {
                String tech = tag.getTechList()[index];
                tagTechs += pre + tech;

                if(index < tag.getTechList().length - 1)
                {
                  tagTechs += separator;
                }
            }
        }
        return tagTechs;
    }

    /**
     * Read type name format of a record.
     * @param tnf The type name format as short.
     * @return The type name format as string.
     */
    private String readTypeNameFormat(short tnf)
    {
        String typeNameFormat;
        switch(tnf)
        {
            case NdefRecord.TNF_ABSOLUTE_URI:
                typeNameFormat = "TNF_ABSOLUTE_URI";
                break;
            case NdefRecord.TNF_EMPTY:
                typeNameFormat = "TNF_EMPTY";
                break;
            case NdefRecord.TNF_EXTERNAL_TYPE:
                typeNameFormat = "TNF_EXTERNAL_TYPE";
                break;
            case NdefRecord.TNF_MIME_MEDIA:
                typeNameFormat = "TNF_MIME_MEDIA";
                break;
            case NdefRecord.TNF_UNCHANGED:
                typeNameFormat = "TNF_UNCHANGED";
                break;
            case NdefRecord.TNF_UNKNOWN:
                typeNameFormat = "TNF_UNKNOWN";
                break;
            case NdefRecord.TNF_WELL_KNOWN:
                typeNameFormat = "TNF_WELL_KNOWN";
                break;
            default:
                typeNameFormat = "Not defined: " + tnf;
                break;
        }

        return typeNameFormat;
    }

    /**
     * Read ID of given NDEF record data.
     * @param id Record ID as byte array.
     * @param separator String used to separate single bytes of the ID.
     * @return Record ID as string.
     */
    private String readID(byte[] id, String separator)
    {
        String idString = "";

        if(id == null || id.length == 0)
        {
            idString = "none";
        }
        else
        {
            for (int index = 0; index < id.length; ++index)
            {
                //TODO: try to read as char (one or two byte)
                idString = idString + id[index];

                if(index < id.length - 1)
                {
                    idString = idString + separator;
                }
            }
        }

        return idString;
    }

    /**
     * Read type information of given NDEF record data.
     * @param type Type information of the record as byte array.
     * @param tnf Type Name Format of the record.
     * @return Type information of the record as string.
     */
    private String readRecordType(byte[] type, short tnf)
    {
        String recordType = "";

        if(tnf == NdefRecord.TNF_WELL_KNOWN)
        {
            if(Arrays.equals(type, NdefRecord.RTD_ALTERNATIVE_CARRIER))
            {
                recordType = "RTD_ALTERNATIVE_CARRIER";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_HANDOVER_CARRIER))
            {
                recordType = "RTD_HANDOVER_CARRIER";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_HANDOVER_REQUEST))
            {
                recordType = "RTD_HANDOVER_REQUEST";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_HANDOVER_SELECT))
            {
                recordType = "RTD_HANDOVER_SELECT";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_SMART_POSTER))
            {
                recordType = "RTD_SMART_POSTER";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_TEXT))
            {
                recordType = "RTD_TEXT";
            }
            else if(Arrays.equals(type, NdefRecord.RTD_URI))
            {
                recordType = "RTD_URI";
            }
            else
            {
                recordType = "Not so well known";
            }
        }
        else
        {
            for(int index = 0; index < type.length; ++index)
            {
                //TODO: try to read as char (one or two byte)
                recordType = recordType + type[index];
            }
        }

        return recordType;
    }

    /**
     * Read the payload of the records into a string.
     * @param records The records to get the payload from.
     * @param charSize Only defined for values 1 and 2.
     * @return The string read from the data.
     */
    private String readDataAsString(NdefRecord[] records, int charSize)
    {
        String output = "";

        for(NdefRecord record : records)
        {
            if(charSize == 1)
            {
                for (byte data : record.getPayload())
                {
                    // In java a char is 16 bit = 2 byte.
                    // Yet for some weird reason, the data may be encoded in single-byte steps.
                    output += (char)data;
                }
            }
            else if (charSize == 2)
            {
                for (int index = 0; index < record.getPayload().length; index += 2)
                {
                    // In java a char is 16 bit = 2 byte.
                    byte first = record.getPayload()[index];
                    byte second = 0;

                    // Crash possible if no check is used.
                    if(records.length > index + 1)
                    {
                        second = record.getPayload()[index + 1];
                    }

                    output += bytesToChar(first, second);
                }
            }
        }

        return output;
    }

    /**
     * Read the payload of the records into a string of hex numbers.
     * @param records The records to get the payload from.
     * @return The string read from the data.
     */
    private String readDataAsHex(NdefRecord[] records)
    {
        String output = "";

        for(NdefRecord record : records)
        {
            for(byte data : record.getPayload())
            {
                output += Integer.toHexString((int)data) + " ";
            }
        }

        return output;
    }

    /**
     * Read the payload of the records into a string of decimal numbers.
     * @param records The records to get the payload from.
     * @return The string read from the data.
     */
    private String readDataAsByte(NdefRecord[] records)
    {
        String output = "";

        for(NdefRecord record : records)
        {
            for(byte data : record.getPayload())
            {
                output += "" + data + " ";
            }
        }

        return output;
    }

    /**
     * Create a character from two bytes.
     * @param first first byte of the char (high byte)
     * @param second second byte of the char (low byte)
     * @return The char made up from the two bytes.
     */
    private char bytesToChar(byte first, byte second)
    {
        // http://www.javacodegeeks.com/2010/11/java-best-practices-char-to-byte-and.html
        return (char)(((first&0x00FF)<<8) + (second&0x00FF));
    }

    /**
     * Read MifareUltralight data.
     * @param tag Tag to be read.
     * @return Data on tag as string.
     */
    private String readMifareTag(Tag tag)
    {
        // Thanks to Adrian for figuring this out. Code taken from:
        // http://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc.html
        MifareUltralight mifare = MifareUltralight.get(tag);
        String output;

        try
        {
            mifare.connect();
            ArrayList<Byte> data = new ArrayList<>();

            // Standard Mifare Ultralight: 16 pages,
            // Mifare Ultralight C: 48 pages officially, yet repeats after 42
            // in both cases: first 4 pages are nfc info, 4 bytes per page
            int pageCount = 42;

            for(int pageIndex = 0; pageIndex < pageCount; pageIndex += 4)
            {
                byte[] payload = mifare.readPages(pageIndex);
                for (int byteIndex = 0; byteIndex < payload.length; ++byteIndex)
                {
                    data.add(payload[byteIndex]);
                }
            }

            mifare.close();
            //output = new String(data., Charset.forName("US-ASCII"));
            output = "Single byte character:\n";
            for(int index = 0; index < data.size(); ++index)
            {
                output += (char)((byte)data.get(index));
            }

            output += "\n\nHexadecimal:\n";

            for(int index = 0; index < data.size(); ++index)
            {
                output += Integer.toHexString((int)data.get(index)) + " ";
            }
            output += "\n";
        }
        catch (IOException iox)
        {
            iox.printStackTrace();
            output = "Error when reading tag:\n";
            output += iox.getMessage() + "\n";
        }

        return output;
    }
}
