// license:BSD-3-Clause
//============================================================
//
// opensl_snd.cpp - Implementation of opensl sound
//
//  MAME4DROID by David Valdeita (Seleuco)
//
//============================================================

#include "opensl_snd.h"

#include <pthread.h>
static pthread_mutex_t sound_mutex     = PTHREAD_MUTEX_INITIALIZER;

static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);
static void queue(unsigned char *p,unsigned size);
static unsigned short dequeue(unsigned char *p,unsigned size);
static inline int emptyQueue(void);

//Size: (44100/30fps) * bytesize * stereo * (3 buffers)
//#define TAM (1470 * 2 * 2 * 3)


#define TAM (2048 * 4 * 4 * 3)

static unsigned char ptr_buf[TAM];
static unsigned head = 0;
static unsigned tail = 0;

inline int fullQueue(unsigned short size){

    if(head < tail)
	{
		return head + size >= tail;
	}
	else if(head > tail)
	{
		return (head + size) >= TAM ? (head + size)- TAM >= tail : 0;
	}
	else return 0;
}

inline int emptyQueue(){
	return head == tail;
}

void queue(unsigned char *p,unsigned size){
        unsigned newhead;
		if(head + size < TAM)
		{
			memcpy(ptr_buf+head,p,size);
			newhead = head + size;
		}
		else
		{
			memcpy(ptr_buf+head,p, TAM -head);
			memcpy(ptr_buf,p + (TAM-head), size - (TAM-head));
			newhead = (head + size) - TAM;
		}

                pthread_mutex_lock(&sound_mutex);

		head = newhead;

		pthread_mutex_unlock(&sound_mutex);
}

unsigned short dequeue(unsigned char *p,unsigned size){

    	unsigned real;
    	unsigned datasize;

		if(emptyQueue())
		{
            memset(p,0,size);
			return size;
		}

		pthread_mutex_lock(&sound_mutex);

		datasize = head > tail ? head - tail : (TAM - tail) + head ;

                pthread_mutex_unlock(&sound_mutex);

                real = datasize > size ? size : datasize;

		if(tail + real < TAM)
		{
			memcpy(p,ptr_buf+tail,real);
			tail+=real;
		}
		else
		{
			memcpy(p,ptr_buf + tail, TAM - tail);
			memcpy(p+ (TAM-tail),ptr_buf , real - (TAM-tail));
			tail = (tail + real) - TAM;
		}

        return real;
}

/*
#define TAM (1600 * 2 * 2 * 3)
unsigned char ptr_buf[TAM];
unsigned head = 0;
unsigned tail = 0;

inline int emptyQueue(){
    return head == tail;
}

void queue(unsigned char *p,unsigned size) {

    unsigned newhead;
    if(head + size < TAM)
    {
        memcpy(ptr_buf+head,p,size);
        newhead = head + size;
    }
    else
    {
        memcpy(ptr_buf+head,p, TAM -head);
        memcpy(ptr_buf,p + (TAM-head), size - (TAM-head));
        newhead = (head + size) - TAM;
    }
    pthread_mutex_lock(&sound_mutex);

    head = newhead;

    pthread_mutex_unlock(&sound_mutex);
}

unsigned short dequeue(unsigned char *p,unsigned size){

    unsigned real;
    unsigned datasize;

    if(emptyQueue())
    {
        memset(p,0,size);
        return 0;
    }

    pthread_mutex_lock(&sound_mutex);

    datasize = head > tail ? head - tail : (TAM - tail) + head ;
    real = datasize > size ? size : datasize;

    if(tail + real < TAM)
    {
        memcpy(p,ptr_buf+tail,real);
        tail+=real;
    }
    else
    {
        memcpy(p,ptr_buf + tail, TAM - tail);
        memcpy(p+ (TAM-tail),ptr_buf , real - (TAM-tail));
        tail = (tail + real) - TAM;
    }

    pthread_mutex_unlock(&sound_mutex);

    if (real < size)
    {
        memset(p+real, 0, size-real);
    }

    return real;
}
*/

static SLresult opensl_createEngine(OPENSL_SND *p)
{
  SLresult result;

  result = slCreateEngine(&(p->engineObject), 0, NULL, 0, NULL, NULL);
  if(result != SL_RESULT_SUCCESS) goto  engine_end;

  result = (*p->engineObject)->Realize(p->engineObject, SL_BOOLEAN_FALSE);
  if(result != SL_RESULT_SUCCESS) goto engine_end;

  result = (*p->engineObject)->GetInterface(p->engineObject, SL_IID_ENGINE, &(p->engineEngine));
  if(result != SL_RESULT_SUCCESS) goto  engine_end;

 engine_end:
  return result;
}

static SLresult opensl_playOpen(OPENSL_SND *p)
{
  SLresult result;
  SLuint32 sr = p->sr;
  SLuint32  channels = p->outchannels;

  if(channels){

    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};

    switch(sr){

    case 8000:
      sr = SL_SAMPLINGRATE_8;
      break;
    case 11025:
      sr = SL_SAMPLINGRATE_11_025;
      break;
    case 16000:
      sr = SL_SAMPLINGRATE_16;
      break;
    case 22050:
      sr = SL_SAMPLINGRATE_22_05;
      break;
    case 24000:
      sr = SL_SAMPLINGRATE_24;
      break;
    case 32000:
      sr = SL_SAMPLINGRATE_32;
      break;
    case 44100:
      sr = SL_SAMPLINGRATE_44_1;
      break;
    case 48000:
      sr = SL_SAMPLINGRATE_48;
      break;
    case 64000:
      sr = SL_SAMPLINGRATE_64;
      break;
    case 88200:
      sr = SL_SAMPLINGRATE_88_2;
      break;
    case 96000:
      sr = SL_SAMPLINGRATE_96;
      break;
    case 192000:
      sr = SL_SAMPLINGRATE_192;
      break;
    default:
      return -1;
    }

    const SLInterfaceID ids[] = {SL_IID_VOLUME};
    const SLboolean req[] = {SL_BOOLEAN_FALSE};
    result = (*p->engineEngine)->CreateOutputMix(p->engineEngine, &(p->outputMixObject), 1, ids, req);
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->outputMixObject)->Realize(p->outputMixObject, SL_BOOLEAN_FALSE);

    SLuint32 speakers;
    if(channels > 1)
      speakers = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    else speakers = SL_SPEAKER_FRONT_CENTER;
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM,channels, sr,
				   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
				   speakers, SL_BYTEORDER_LITTLEENDIAN};

    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, p->outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req1[] = {SL_BOOLEAN_TRUE};
    result = (*p->engineEngine)->CreateAudioPlayer(p->engineEngine, &(p->bqPlayerObject), &audioSrc, &audioSnk,
						   1, ids1, req1);
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->bqPlayerObject)->Realize(p->bqPlayerObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_PLAY, &(p->bqPlayerPlay));
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->bqPlayerObject)->GetInterface(p->bqPlayerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
						&(p->bqPlayerBufferQueue));
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->bqPlayerBufferQueue)->RegisterCallback(p->bqPlayerBufferQueue, bqPlayerCallback, p);
    if(result != SL_RESULT_SUCCESS) return result;

    result = (*p->bqPlayerPlay)->SetPlayState(p->bqPlayerPlay, SL_PLAYSTATE_PLAYING);

    if((p->playBuffer[0] = (short *) calloc(p->outBufSamples, sizeof(short))) == NULL) {
      return -1;
    }

    if((p->playBuffer[1] = (short *) calloc(p->outBufSamples, sizeof(short))) == NULL) {
      return -1;
    }

    p->currPlayBuffer = 0;

    (*p->bqPlayerBufferQueue)->Enqueue(p->bqPlayerBufferQueue,
				       p->playBuffer[p->currPlayBuffer],p->outBufSamples*sizeof(short));
  }
  return SL_RESULT_SUCCESS;
}

static void opensl_destroyEngine(OPENSL_SND *p){

  if (p->bqPlayerObject != NULL) {
    SLuint32 state = SL_PLAYSTATE_PLAYING;
    (*p->bqPlayerPlay)->SetPlayState(p->bqPlayerPlay, SL_PLAYSTATE_STOPPED);
    while(state != SL_PLAYSTATE_STOPPED)
      (*p->bqPlayerPlay)->GetPlayState(p->bqPlayerPlay, &state);
    (*p->bqPlayerObject)->Destroy(p->bqPlayerObject);
    p->bqPlayerObject = NULL;
    p->bqPlayerPlay = NULL;
    p->bqPlayerBufferQueue = NULL;
  }

  if (p->outputMixObject != NULL) {
    (*p->outputMixObject)->Destroy(p->outputMixObject);
    p->outputMixObject = NULL;
  }

  if (p->engineObject != NULL) {
    (*p->engineObject)->Destroy(p->engineObject);
    p->engineObject = NULL;
    p->engineEngine = NULL;
  }
}

OPENSL_SND *opensl_open(int sr, int outchannels, int bufferframes){

  OPENSL_SND *p;
  p = (OPENSL_SND *) malloc(sizeof(OPENSL_SND));
  memset(p, 0, sizeof(OPENSL_SND));
  p->outchannels = outchannels;
  p->sr = sr;

  if((p->outBufSamples  =  bufferframes*outchannels) != 0) {
    if((p->outputBuffer = (short *) calloc(p->outBufSamples, sizeof(short))) == NULL) {
      opensl_close(p);
      return NULL;
    }
  }

  if(opensl_createEngine(p) != SL_RESULT_SUCCESS) {
    opensl_close(p);
    return NULL;
  }

  if(opensl_playOpen(p) != SL_RESULT_SUCCESS) {
    opensl_close(p);
    return NULL;
  }

  return p;
}

void opensl_close(OPENSL_SND *p){

  if (p == NULL)
    return;

  opensl_destroyEngine(p);

  if (p->outputBuffer != NULL) {
    free(p->outputBuffer);
    p->outputBuffer= NULL;
  }

  if (p->playBuffer[0] != NULL) {
    free(p->playBuffer[0]);
    p->playBuffer[0] = NULL;
  }

  if (p->playBuffer[1] != NULL) {
    free(p->playBuffer[1]);
    p->playBuffer[1] = NULL;
  }

  free(p);
}

void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
  OPENSL_SND *p = (OPENSL_SND *) context;
  int bytes = p->outBufSamples*sizeof(short);

  dequeue((unsigned char *)p->playBuffer[p->currPlayBuffer],bytes);
 (*p->bqPlayerBufferQueue)->Enqueue(p->bqPlayerBufferQueue,p->playBuffer[p->currPlayBuffer],bytes);
  p->currPlayBuffer = (p->currPlayBuffer + 1) % 2;
}

int opensl_write(OPENSL_SND *p, short *buffer,int size){

  int bytes = size*sizeof(short);
  if(p == NULL  ||  p->outBufSamples ==  0)  return 0;

  queue((unsigned char *)buffer,size*sizeof(short));

  return bytes/sizeof(short);
}

