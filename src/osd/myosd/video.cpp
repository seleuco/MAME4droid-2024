// license:BSD-3-Clause
//============================================================
//
//  video.cpp -  osd video handling
//
//  MAME4DROID by David Valdeita (Seleuco)
//
//============================================================

// MAME headers
#include "emu.h"
#include "render.h"
#include "rendlay.h"
#include "ui/uimain.h"
#include "ui/ui.h"
#include "mame.h"
#include "ui/menu.h"

#include "drivenum.h"
#include "rendersw.hxx"

//MYOSD headers
#include "myosd.h"

#define MIN(a,b) ((a)<(b) ? (a) : (b))
#define MAX(a,b) ((a)<(b) ? (b) : (a))

// myosd_screen_ptr - needed 4 SW renderer
uint8_t *myosd_screen_ptr;
int myosd_fps;
int myosd_zoom_to_window;

//============================================================
//  video_init
//============================================================

void my_osd_interface::video_init()
{
    osd_printf_verbose("my_osd_interface::video_init\n");

    // create our *single* render target, we dont do multiple windows or monitors
    m_target = machine().render().target_alloc();

    m_video_none = strcmp(options().value(OPTION_VIDEO), "none") == 0;

    m_min_width = 0;
    m_min_height = 0;
    m_vis_width = 0;
    m_vis_height = 0;
}

//============================================================
//  video_exit
//============================================================

void my_osd_interface::video_exit()
{
    osd_printf_verbose("my_osd_interface::video_exit\n");

    // free the render target
    machine().render().target_free(m_target);
    m_target = nullptr;

    if (m_callbacks.video_exit != nullptr)
        m_callbacks.video_exit();
}


//============================================================
//  update
//============================================================

void my_osd_interface::update(bool skip_redraw)
{
    osd_printf_verbose("my_osd_interface::update\n");

    if(m_callbacks.video_draw == nullptr)
        return;

    bool in_game = /*machine().phase() == machine_phase::RUNNING &&*/ &(machine().system()) != &GAME_NAME(___empty);
    bool in_menu = /*machine().phase() == machine_phase::RUNNING &&*/ machine().ui().is_menu_active();
    bool running = machine().phase() == machine_phase::RUNNING;
    mame_machine_manager::instance()->ui().set_show_fps(myosd_fps);

    // if skipping this redraw, bail
    if (!skip_redraw && !m_video_none) {

        int vis_width, vis_height;
        int min_width, min_height;

        //__android_log_print(ANDROID_LOG_DEBUG, "libMAME4droid.so", "video min_width:%d min_height:%d",min_width,min_height);

        //target()->compute_visible_area(MAX(640,myosd_display_width), MAX(480,myosd_display_height), 1.0, target()->orientation(), vis_width, vis_height);

        bool autores = myosd_display_width == 0 && myosd_display_height == 0;

        if (in_game && (myosd_zoom_to_window || autores)) {

            if (!autores) {

                target()->compute_visible_area(myosd_display_width, myosd_display_height, 1.0,
                                               target()->orientation(), vis_width, vis_height);

                min_width = vis_width;
                min_height = vis_height;
            } else {

                target()->compute_minimum_size( min_width, min_height);
                if(min_width>640)min_width=640;
                if(min_height>480)min_height=480;

                target()->set_keepaspect(true);

                target()->compute_visible_area(min_width, min_height, 1.0,
                                               target()->orientation(), vis_width, vis_height);

                target()->set_keepaspect(false);

            }

        } else {
            if (in_game) {
                min_width = vis_width = myosd_display_width;
                min_height = vis_height = myosd_display_height;
            } else {
                min_width = vis_width = myosd_display_width_osd;
                min_height = vis_height = myosd_display_height_osd;
            }
        }

        // check for a change in the min-size of render target *or* size of the vis screen
        if (min_width != m_min_width || min_height != m_min_height
             || vis_width != m_vis_width || vis_height != m_vis_height) {

            m_min_width = min_width;
            m_min_height = min_height;
            m_vis_width = vis_width;
            m_vis_height = vis_height;

            if (m_callbacks.video_init != nullptr) {
                m_callbacks.video_init(min_width, min_height, vis_width, vis_height);
            }
        }

        target()->set_bounds(min_width, min_height);

        render_primitive_list *primlist = &target()->get_primitives();

        int const pitch = min_width;

        primlist->acquire_lock();
        //bgr888
        software_renderer<uint32_t, 0, 0, 0, 0, 8, 16>::draw_primitives(*primlist, myosd_screen_ptr,
                                                                        min_width,
                                                                        min_height,
                                                                        pitch);

        primlist->release_lock();
    }

    m_callbacks.video_draw(skip_redraw || m_video_none, in_game, in_menu, running);
}


