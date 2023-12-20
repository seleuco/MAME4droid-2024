// license:BSD-3-Clause
//============================================================
//
//  sound.cpp -  osd sound handling
//
//  MAME4DROID by David Valdeita (Seleuco)
//
//============================================================

// MAME headers
#include "emu.h"

// DROID headers
#include "myosd.h"

static void myosd_sound_init(int rate, int stereo);
static void myosd_sound_play(void *buff, int len);
static void myosd_sound_exit(void);

//============================================================
//  sound_init
//============================================================

void my_osd_interface::sound_init()
{
    osd_printf_verbose("droid_osd_interface::sound_init\n");

    // if the host does not want to handle audio, do a default
    if (m_callbacks.sound_play == NULL)
    {
        m_callbacks.sound_init = myosd_sound_init;
        m_callbacks.sound_play = myosd_sound_play;
        m_callbacks.sound_exit = myosd_sound_exit;
    }

    m_sample_rate = options().sample_rate();

    if (strcmp(options().value(OPTION_SOUND), "none") == 0)
        m_sample_rate = 0;

    if (m_sample_rate != 0)
    {
        // set the startup volume
        set_mastervolume(0);
        m_callbacks.sound_init(m_sample_rate, 1);
    }
}

//============================================================
//  sound_exit
//============================================================

void my_osd_interface::sound_exit()
{
    osd_printf_verbose("droid_osd_interface::sound_exit\n");
    if (m_sample_rate != 0)
    {
        m_callbacks.sound_exit();
    }
}

//============================================================
//    Apply attenuation
//============================================================

static void att_memcpy(void *dest, const int16_t *data, int bytes_to_copy, int attenuation)
{
    int level = (int)(pow(10.0, (float) attenuation / 20.0) * 128.0);
    int16_t *d = (int16_t *)dest;
    int count = bytes_to_copy/2;
    while (count>0)
    {
        *d++ = (*data++ * level) >> 7; /* / 128 */
        count--;
    }
}

//============================================================
//    osd_update_audio_stream
//============================================================

void my_osd_interface::update_audio_stream(const int16_t *buffer, int samples_this_frame)
{
    osd_printf_verbose("my_osd_interface::update_audio_stream: samples=%d attenuation=%d\n", samples_this_frame, m_attenuation);

    static unsigned char bufferatt[882*2*2*10];

    if(machine().video().fastforward())
        return;

    if (m_sample_rate != 0 && m_attenuation != -32/*muted*/)
    {
        if (m_attenuation != 0)
        {
            if (samples_this_frame * 2 * 2 >= sizeof(bufferatt))
                samples_this_frame = sizeof(bufferatt) / (2 * 2);

            att_memcpy(bufferatt,buffer,samples_this_frame * sizeof(int16_t) * 2, m_attenuation);
            buffer = (int16_t *)bufferatt;
        }
        m_callbacks.sound_play((void*)buffer,samples_this_frame * sizeof(int16_t) * 2);
    }
}

void my_osd_interface::set_mastervolume(int attenuation)
{
    // clamp the attenuation to 0-32 range
    if (attenuation > 0)
        attenuation = 0;
    if (attenuation < -32)
        attenuation = -32;

    m_attenuation = attenuation;
    //m_attenuation_level = (int)(pow(10.0, (float)m_attenuation / 20.0) * 128.0);
}

bool my_osd_interface::no_sound()
{
    return m_sample_rate == 0;
}

//============================================================
//  default sound impl
//============================================================


static void myosd_sound_init(int rate, int stereo)
{

}

static void myosd_sound_exit(void)
{

}

static void myosd_sound_play(void *buff, int len)
{

}
