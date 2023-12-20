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

    // if skipping this redraw, bail
    if (skip_redraw || m_callbacks.video_draw == nullptr || m_video_none)
        return;

    mame_machine_manager::instance()->ui().set_show_fps(myosd_fps);

    bool in_game = &(machine().system()) != &GAME_NAME(___empty);
    //bool in_menu = in_game && machine().phase() == machine_phase::RUNNING && machine().ui().is_menu_active();
    bool in_menu = machine().phase() == machine_phase::RUNNING && machine().ui().is_menu_active();

    int vis_width, vis_height;
    int min_width, min_height;
    target()->compute_minimum_size(min_width, min_height);
    //target()->compute_visible_area(MAX(640,myosd_display_width), MAX(480,myosd_display_height), 1.0, target()->orientation(), vis_width, vis_height);

    if(in_game) {
        target()->compute_visible_area(myosd_display_width, myosd_display_height, 1.0,
                                       target()->orientation(), vis_width, vis_height);
    }
    else
    {
        //vis_width = MAX(640,myosd_display_width);
        //vis_height = MAX(480,myosd_display_height);
        vis_width = myosd_display_width;
        vis_height = myosd_display_height;
    }

    // check for a change in the min-size of render target *or* size of the vis screen
    if (/*min_width != m_min_width || min_height != m_min_height |*/ vis_width != m_vis_width || vis_height != m_vis_height) {

        m_min_width = min_width;
        m_min_height = min_height;
        m_vis_width = vis_width;
        m_vis_height = vis_height;

        if (m_callbacks.video_init != nullptr) {
            m_callbacks.video_init( vis_width, vis_height);
        }
    }

    target()->set_bounds(vis_width, vis_height);

    render_primitive_list *primlist = &target()->get_primitives();

    int const pitch = vis_width;
    //int const pitch = m_min_width;

    primlist->acquire_lock();
    //bgr888
    software_renderer<uint32_t, 0,0,0, 0,8,16>::draw_primitives(*primlist, myosd_screen_ptr,  vis_width, vis_height, pitch);
    primlist->release_lock();

    m_callbacks.video_draw(in_game, in_menu);
}


